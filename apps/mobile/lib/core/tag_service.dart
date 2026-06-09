import 'api_client.dart';

class TagService {
  static Future<List<dynamic>> list() async {
    final res = await ApiClient.get('/tags');
    return res['data']['items'];
  }

  static Future<Map<String, dynamic>> create(String name, {String? color}) async {
    final res = await ApiClient.post('/tags', body: {'name': name, if (color != null) 'color': color});
    return res['data'];
  }

  static Future<void> delete(String id) async => await ApiClient.delete('/tags/$id');
}
