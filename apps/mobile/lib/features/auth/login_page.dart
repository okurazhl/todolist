import 'package:flutter/material.dart';
import '../../core/auth_service.dart';
import '../../core/api_client.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _username = TextEditingController();
  final _password = TextEditingController();
  bool _loading = false;
  String? _error;

  Future<void> _login() async {
    setState(() { _loading = true; _error = null; });
    try {
      await AuthService.login(_username.text, _password.text);
      if (mounted) Navigator.pushReplacementNamed(context, '/');
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } catch (e) {
      setState(() => _error = '网络错误');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('登录')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            TextField(controller: _username, decoration: const InputDecoration(labelText: '用户名')),
            const SizedBox(height: 12),
            TextField(controller: _password, obscureText: true, decoration: const InputDecoration(labelText: '密码')),
            const SizedBox(height: 16),
            if (_error != null) Text(_error!, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 8),
            SizedBox(width: double.infinity, child: ElevatedButton(onPressed: _loading ? null : _login, child: Text(_loading ? '登录中...' : '登录'))),
            TextButton(onPressed: () => Navigator.pushNamed(context, '/register'), child: const Text('没有账号？注册')),
          ],
        ),
      ),
    );
  }
}
