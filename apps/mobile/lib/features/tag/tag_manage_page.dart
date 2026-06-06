import 'package:flutter/material.dart';
import '../../core/tag_service.dart';

class TagManagePage extends StatefulWidget {
  const TagManagePage({super.key});
  @override State<TagManagePage> createState() => _TagManagePageState();
}

class _TagManagePageState extends State<TagManagePage> {
  List<dynamic> _tags = [];
  final _name = TextEditingController();

  @override void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    final items = await TagService.list();
    setState(() => _tags = items);
  }

  Future<void> _create() async {
    if (_name.text.isEmpty) return;
    await TagService.create(_name.text);
    _name.clear();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('标签管理')),
      body: Column(children: [
        Padding(padding: const EdgeInsets.all(16), child: Row(children: [
          Expanded(child: TextField(controller: _name, decoration: const InputDecoration(labelText: '新标签名'))),
          const SizedBox(width: 8),
          ElevatedButton(onPressed: _create, child: const Text('添加')),
        ])),
        Expanded(child: ListView.builder(
          itemCount: _tags.length,
          itemBuilder: (_, i) => ListTile(
            title: Text(_tags[i]['name']),
            trailing: IconButton(icon: const Icon(Icons.delete), onPressed: () async {
              await TagService.delete(_tags[i]['id']);
              _load();
            }),
          ),
        )),
      ]),
    );
  }
}
