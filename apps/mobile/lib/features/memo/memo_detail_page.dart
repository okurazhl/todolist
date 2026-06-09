import 'package:flutter/material.dart';
import '../../core/memo_service.dart';

class MemoDetailPage extends StatefulWidget {
  final String memoId;
  const MemoDetailPage({super.key, required this.memoId});
  @override State<MemoDetailPage> createState() => _MemoDetailPageState();
}

class _MemoDetailPageState extends State<MemoDetailPage> {
  Map<String, dynamic>? _memo;
  bool _loading = true;

  @override void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final m = await MemoService.getById(widget.memoId);
      setState(() => _memo = m);
    } catch (_) {}
    finally { if (mounted) setState(() => _loading = false); }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Scaffold(body: Center(child: CircularProgressIndicator()));
    if (_memo == null) return const Scaffold(body: Center(child: Text('备忘录不存在')));

    final m = _memo!;
    return Scaffold(
      appBar: AppBar(title: Text(m['title'] ?? ''), actions: [
        IconButton(icon: const Icon(Icons.edit), onPressed: () async {
          await Navigator.pushNamed(context, '/memo/edit', arguments: m['id']);
          _load();
        }),
      ]),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          if (m['isPinned'] == true) const Chip(label: Text('📌 置顶')),
          if (m['categoryId'] != null) Chip(label: Text('分类: ${m['categoryId']}')),
          const Divider(),
          Expanded(child: SingleChildScrollView(child: Text(m['content'] ?? '(无内容)', style: const TextStyle(fontSize: 16)))),
        ]),
      ),
    );
  }
}
