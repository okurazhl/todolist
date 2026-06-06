import 'api_client.dart';

class AuthService {
  static Future<Map<String, dynamic>> login(String username, String password) async {
    final res = await ApiClient.post('/auth/login', body: {
      'username': username,
      'password': password,
    });
    final data = res['data'];
    await ApiClient.saveTokens(data['accessToken'], data['refreshToken']);
    return data;
  }

  static Future<Map<String, dynamic>> register(String username, String password, {String? email}) async {
    final res = await ApiClient.post('/auth/register', body: {
      'username': username,
      'password': password,
      if (email != null) 'email': email,
    });
    return res['data'];
  }

  static Future<void> logout() async {
    await ApiClient.clearTokens();
  }
}
