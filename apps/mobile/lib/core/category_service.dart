import 'api_client.dart';

class CategoryService {
  static Future<List<dynamic>> list() async {
    final res = await ApiClient.get('/categories');
    return res['data']['items'];
  }

  static Future<Map<String, dynamic>> create(String name, {String? color}) async {
    final res = await ApiClient.post('/categories', body: {'name': name, 'sortOrder': 0, if (color != null) 'color': color});
    return res['data'];
  }

  static Future<void> delete(String id) async => await ApiClient.delete('/categories/$id');
}
