import 'package:flutter/material.dart';
import 'core/api_client.dart';
import 'app.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await ApiClient.loadTokens();
  runApp(const SmartMemoApp());
}
