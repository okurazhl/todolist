import 'package:flutter/material.dart';
import '../../core/memo_service.dart';
import '../../core/auth_service.dart';

class MemoListPage extends StatefulWidget {
  const MemoListPage({super.key});
  @override State<MemoListPage> createState() => _MemoListPageState();
}

class _MemoListPageState extends State<MemoListPage> {
  List<dynamic> _memos = [];
  bool _loading = true;
  String _status = 'active';

  @override void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final items = await MemoService.list(status: _status);
      setState(() => _memos = items);
    } catch (_) { /* ignore */ }
    finally { if (mounted) setState(() => _loading = false); }
  }

  Future<void> _logout() async {
    await AuthService.logout();
    if (mounted) Navigator.pushReplacementNamed(context, '/login');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('备忘录'),
        actions: [
          PopupMenuButton<String>(itemBuilder: (_) => [
            const PopupMenuItem(value: 'active', child: Text('活跃')),
            const PopupMenuItem(value: 'archived', child: Text('已归档')),
          ], onSelected: (v) { _status = v; _load(); }),
          IconButton(icon: const Icon(Icons.logout), onPressed: _logout),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView.builder(
              itemCount: _memos.length,
              itemBuilder: (_, i) {
                final m = _memos[i];
                return ListTile(
                  leading: m['isPinned'] == true ? const Icon(Icons.push_pin, size: 18) : null,
                  title: Text(m['title'] ?? '', maxLines: 1),
                  subtitle: Text((m['content'] ?? '').toString().length > 50 ? '${(m['content'] as String).substring(0, 50)}...' : (m['content'] ?? '')),
                  trailing: PopupMenuButton<String>(
                    itemBuilder: (_) => [
                      const PopupMenuItem(value: 'pin', child: Text('置顶/取消')),
                      const PopupMenuItem(value: 'archive', child: Text('归档')),
                      const PopupMenuItem(value: 'delete', child: Text('删除')),
                    ],
                    onSelected: (action) async {
                      if (action == 'pin') { m['isPinned'] == true ? await MemoService.unpin(m['id']) : await MemoService.pin(m['id']); _load(); }
                      if (action == 'archive') { await MemoService.archive(m['id']); _load(); }
                      if (action == 'delete') { await MemoService.delete(m['id']); _load(); }
                    },
                  ),
                  onTap: () async {
                    await Navigator.pushNamed(context, '/memo/detail', arguments: m['id']);
                    _load();
                  },
                );
              },
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await Navigator.pushNamed(context, '/memo/edit');
          _load();
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
