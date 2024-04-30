import 'package:flutter/material.dart';
import 'package:liveness/screen/home_screen.dart';
import 'package:liveness/screen/liveness_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Liveness',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      initialRoute: "/",
      routes: {
        "/": (context) => const HomeScreen(),
        "/liveness": (context) => const LivenessScreen()
      },
    );
  }
}
