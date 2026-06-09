import 'package:flutter_test/flutter_test.dart';

import 'package:smartmemo_mobile/app.dart';

void main() {
  testWidgets('App renders', (WidgetTester tester) async {
    await tester.pumpWidget(const SmartMemoApp());
    expect(find.text('登录'), findsOneWidget);
  });
}
