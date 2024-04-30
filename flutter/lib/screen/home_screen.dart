import 'dart:typed_data';

import 'package:flutter/material.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<Uint8List>? images;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          Expanded(
            child: GridView(
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2),
              children: images
                      ?.map((bytes) => Image.memory(bytes, fit: BoxFit.cover))
                      .toList() ??
                  [],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: SizedBox(
              width: double.infinity,
              child: FilledButton(
                  onPressed: () async {
                    final result =
                        await Navigator.pushNamed(context, "/liveness");
                    setState(() {
                      images = result as List<Uint8List>?;
                    });
                  },
                  child: const Text("Start Liveness")),
            ),
          )
        ],
      ),
    );
  }
}
