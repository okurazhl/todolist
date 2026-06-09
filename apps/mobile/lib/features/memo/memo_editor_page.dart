import 'package:flutter/material.dart';
import '../../core/memo_service.dart';
import '../../core/tag_service.dart';
import '../../core/category_service.dart';

class MemoEditorPage extends StatefulWidget {
  final String? memoId;
  const MemoEditorPage({super.key, this.memoId});
  @override State<MemoEditorPage> createState() => _MemoEditorPageState();
}

class _MemoEditorPageState extends State<MemoEditorPage> {
  final _title = TextEditingController();
  final _content = TextEditingController();
  String? _categoryId;
  List<String> _selectedTags = [];
  List<dynamic> _tags = [];
  List<dynamic> _categories = [];
  bool _loading = true;
  bool _saving = false;

  bool get isEdit => widget.memoId != null;

  @override void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final results = await Future.wait([TagService.list(), CategoryService.list()]);
    _tags = results[0];
    _categories = results[1];

    if (isEdit) {
      final m = await MemoService.getById(widget.memoId!);
      _title.text = m['title'] ?? '';
      _content.text = m['content'] ?? '';
      _categoryId = m['categoryId'];
      _selectedTags = List<String>.from(m['tagIds'] ?? []);
    }
    setState(() => _loading = false);
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    try {
      if (isEdit) {
        await MemoService.update(widget.memoId!, title: _title.text, content: _content.text, categoryId: _categoryId, tagIds: _selectedTags);
      } else {
        await MemoService.create(title: _title.text, content: _content.text, categoryId: _categoryId, tagIds: _selectedTags);
      }
      if (mounted) Navigator.pop(context);
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('保存失败')));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Scaffold(body: Center(child: CircularProgressIndicator()));

    return Scaffold(
      appBar: AppBar(title: Text(isEdit ? '编辑备忘录' : '新建备忘录'), actions: [
        IconButton(icon: const Icon(Icons.check), onPressed: _saving ? null : _save),
      ]),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: ListView(children: [
          TextField(controller: _title, decoration: const InputDecoration(labelText: '标题')),
          const SizedBox(height: 12),
          TextField(controller: _content, maxLines: 8, decoration: const InputDecoration(labelText: '正文')),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _categoryId, decoration: const InputDecoration(labelText: '分类'),
            items: [const DropdownMenuItem<String>(value: null, child: Text('无分类')), ..._categories.map<DropdownMenuItem<String>>((c) => DropdownMenuItem<String>(value: c['id'] as String, child: Text(c['name'] as String)))],
            onChanged: (v) => setState(() => _categoryId = v),
          ),
          const SizedBox(height: 12),
          Wrap(spacing: 8, children: _tags.map((t) => FilterChip(
            label: Text(t['name']),
            selected: _selectedTags.contains(t['id']),
            onSelected: (v) {
              setState(() { if (v) { _selectedTags.add(t['id']); } else { _selectedTags.remove(t['id']); } });
            },
          )).toList()),
          const SizedBox(height: 24),
          ElevatedButton(onPressed: _saving ? null : _save, child: Text(_saving ? '保存中...' : '保存')),
        ]),
      ),
    );
  }
}
