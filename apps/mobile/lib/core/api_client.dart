import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiClient {
  static const String _baseUrl = 'http://10.0.2.2:8080/api/v1';
  static const String _tokenKey = 'access_token';
  static const String _refreshKey = 'refresh_token';

  static String? _accessToken;
  static String? _refreshToken;

  static Future<void> loadTokens() async {
    final prefs = await SharedPreferences.getInstance();
    _accessToken = prefs.getString(_tokenKey);
    _refreshToken = prefs.getString(_refreshKey);
  }

  static Future<void> saveTokens(String access, String refresh) async {
    _accessToken = access;
    _refreshToken = refresh;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, access);
    await prefs.setString(_refreshKey, refresh);
  }

  static Future<void> clearTokens() async {
    _accessToken = null;
    _refreshToken = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_refreshKey);
  }

  static bool get isLoggedIn => _accessToken != null;

  static Future<Map<String, dynamic>> get(String path, {Map<String, String>? params}) async {
    final uri = Uri.parse('$_baseUrl$path').replace(queryParameters: params);
    final headers = _buildHeaders();
    final res = await http.get(uri, headers: headers);
    return _handleResponse(res);
  }

  static Future<Map<String, dynamic>> post(String path, {Map<String, dynamic>? body}) async {
    final uri = Uri.parse('$_baseUrl$path');
    final headers = _buildHeaders();
    final res = await http.post(uri, headers: headers, body: jsonEncode(body));
    return _handleResponse(res);
  }

  static Future<Map<String, dynamic>> patch(String path, {Map<String, dynamic>? body}) async {
    final uri = Uri.parse('$_baseUrl$path');
    final headers = _buildHeaders();
    final res = await http.patch(uri, headers: headers, body: jsonEncode(body));
    return _handleResponse(res);
  }

  static Future<Map<String, dynamic>> delete(String path) async {
    final uri = Uri.parse('$_baseUrl$path');
    final headers = _buildHeaders();
    final res = await http.delete(uri, headers: headers);
    return _handleResponse(res);
  }

  static Map<String, String> _buildHeaders() {
    final h = <String, String>{'Content-Type': 'application/json'};
    if (_accessToken != null) h['Authorization'] = 'Bearer $_accessToken';
    return h;
  }

  static Future<Map<String, dynamic>> _handleResponse(http.Response res) async {
    final body = jsonDecode(res.body) as Map<String, dynamic>;
    if (res.statusCode == 401) {
      final refreshed = await _tryRefresh();
      if (refreshed) return body; // caller should retry
      throw ApiException(body['code'] ?? 'AUTH_ERROR', body['message'] ?? '认证失败');
    }
    if (res.statusCode >= 400) {
      throw ApiException(body['code'] ?? 'ERROR', body['message'] ?? '请求失败');
    }
    return body;
  }

  static Future<bool> _tryRefresh() async {
    if (_refreshToken == null) return false;
    try {
      final res = await http.post(
        Uri.parse('$_baseUrl/auth/refresh'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'refreshToken': _refreshToken}),
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body)['data'];
        await saveTokens(data['accessToken'], data['refreshToken']);
        return true;
      }
    } catch (_) {}
    await clearTokens();
    return false;
  }
}

class ApiException implements Exception {
  final String code;
  final String message;
  ApiException(this.code, this.message);
  @override
  String toString() => '$code: $message';
}
