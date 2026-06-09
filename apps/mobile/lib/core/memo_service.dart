import 'api_client.dart';

class MemoService {
  static Future<List<dynamic>> list({String? status, String? categoryId, String? cursor, int limit = 20}) async {
    final params = <String, String>{'limit': '$limit'};
    if (status != null) params['status'] = status;
    if (categoryId != null) params['categoryId'] = categoryId;
    if (cursor != null) params['cursor'] = cursor;
    final res = await ApiClient.get('/memos', params: params);
    return res['data']['items'];
  }

  static Future<Map<String, dynamic>> getById(String id) async {
    final res = await ApiClient.get('/memos/$id');
    return res['data'];
  }

  static Future<Map<String, dynamic>> create({
    required String title, String? content, String? categoryId, List<String>? tagIds, bool? isPinned,
  }) async {
    final res = await ApiClient.post('/memos', body: {
      'title': title,
      if (content != null) 'content': content,
      if (categoryId != null) 'categoryId': categoryId,
      if (tagIds != null) 'tagIds': tagIds,
      if (isPinned != null) 'isPinned': isPinned,
    });
    return res['data'];
  }

  static Future<Map<String, dynamic>> update(String id, {String? title, String? content, String? categoryId, List<String>? tagIds, bool? isPinned}) async {
    final res = await ApiClient.patch('/memos/$id', body: {
      if (title != null) 'title': title,
      if (content != null) 'content': content,
      if (categoryId != null) 'categoryId': categoryId,
      if (tagIds != null) 'tagIds': tagIds,
      if (isPinned != null) 'isPinned': isPinned,
    });
    return res['data'];
  }

  static Future<void> delete(String id) async => await ApiClient.delete('/memos/$id');

  static Future<Map<String, dynamic>> pin(String id) async => (await ApiClient.post('/memos/$id/pin'))['data'];
  static Future<Map<String, dynamic>> unpin(String id) async => (await ApiClient.delete('/memos/$id/pin'))['data'];
  static Future<Map<String, dynamic>> archive(String id) async => (await ApiClient.post('/memos/$id/archive'))['data'];
}
