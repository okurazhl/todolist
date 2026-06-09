import 'package:flutter/material.dart';
import '../../core/category_service.dart';

class CategoryManagePage extends StatefulWidget {
  const CategoryManagePage({super.key});
  @override State<CategoryManagePage> createState() => _CategoryManagePageState();
}

class _CategoryManagePageState extends State<CategoryManagePage> {
  List<dynamic> _categories = [];
  final _name = TextEditingController();

  @override void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    final items = await CategoryService.list();
    setState(() => _categories = items);
  }

  Future<void> _create() async {
    if (_name.text.isEmpty) return;
    await CategoryService.create(_name.text);
    _name.clear();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('分类管理')),
      body: Column(children: [
        Padding(padding: const EdgeInsets.all(16), child: Row(children: [
          Expanded(child: TextField(controller: _name, decoration: const InputDecoration(labelText: '新分类名'))),
          const SizedBox(width: 8),
          ElevatedButton(onPressed: _create, child: const Text('添加')),
        ])),
        Expanded(child: ListView.builder(
          itemCount: _categories.length,
          itemBuilder: (_, i) => ListTile(
            title: Text(_categories[i]['name']),
            trailing: IconButton(icon: const Icon(Icons.delete), onPressed: () async {
              await CategoryService.delete(_categories[i]['id']);
              _load();
            }),
          ),
        )),
      ]),
    );
  }
}
