import 'package:flutter/material.dart';
import 'core/api_client.dart';
import 'features/auth/login_page.dart';
import 'features/auth/register_page.dart';
import 'features/memo/memo_list_page.dart';
import 'features/memo/memo_detail_page.dart';
import 'features/memo/memo_editor_page.dart';
import 'features/tag/tag_manage_page.dart';
import 'features/category/category_manage_page.dart';

class SmartMemoApp extends StatelessWidget {
  const SmartMemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '智能备忘录',
      theme: ThemeData(
        colorSchemeSeed: Colors.blue,
        useMaterial3: true,
      ),
      initialRoute: ApiClient.isLoggedIn ? '/' : '/login',
      routes: {
        '/login': (_) => const LoginPage(),
        '/register': (_) => const RegisterPage(),
        '/': (_) => const MemoListPage(),
        '/tags': (_) => const TagManagePage(),
        '/categories': (_) => const CategoryManagePage(),
      },
      onGenerateRoute: (settings) {
        if (settings.name == '/memo/detail') {
          final id = settings.arguments as String;
          return MaterialPageRoute(builder: (_) => MemoDetailPage(memoId: id));
        }
        if (settings.name == '/memo/edit') {
          final id = settings.arguments as String?;
          return MaterialPageRoute(builder: (_) => MemoEditorPage(memoId: id));
        }
        return null;
      },
    );
  }
}
