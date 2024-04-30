import 'dart:developer';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';
import 'package:liveness/liveness/states.dart';
import 'package:liveness/liveness/utils.dart';
import 'package:wakelock/wakelock.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';

enum _FaceError { noFace, multiFace }

class LivenessScreen extends StatefulWidget {
  const LivenessScreen({super.key});

  @override
  State<LivenessScreen> createState() => _LivenessScreenState();
}

class _LivenessScreenState extends State<LivenessScreen>
    with WidgetsBindingObserver {
  _FaceError? _faceError;
  final _stateText = ValueNotifier("");
  bool _isProcessingImage = false;
  final List<XFile> _photos = [];
  double _imageWidth = 0;
  double _imageHeight = 0;
  bool _needUpdateImageInfo = true;
  int _consecutiveEmptyFacesTimes = 0;

  CameraController? _controller;
  CameraDescription? _camera;

  final FaceDetector _faceDetector = FaceDetector(
    options: FaceDetectorOptions(
      enableContours: true,
      enableLandmarks: true,
      enableClassification: true,
      minFaceSize: 0.9,
      enableTracking: false,
      performanceMode: FaceDetectorMode.fast,
    ),
  );

  late LivenessContext _livenessContext;

  _LivenessScreenState() {
    _livenessContext = LivenessContext(
      states: [
        FrontFaceState(10),
        SmileState(1),
        SideFaceState(1),
        MouthOpenState(1),
      ],
      stateChangeCallback: ({required next, required retry}) async {
        final file = await _takePhoto();
        if (file != null) {
          _photos.add(file);
          next();
        } else {
          retry();
        }
      },
      onCompleted: (photoCount) async {
        final List<Uint8List> compressedPhotos = [];
        if (_photos.length >= photoCount) {
          final photos =
              _photos.sublist(_photos.length - photoCount, _photos.length);
          for (final photo in photos) {
            final bytes = await FlutterImageCompress.compressWithFile(
                photo.path,
                quality: 75,
                autoCorrectionAngle: true);
            if (bytes == null) {
              if (mounted) {
                Navigator.pop(context);
              }
              return;
            } else {
              compressedPhotos.add(bytes);
            }
          }
          if (mounted) {
            Navigator.pop(context, compressedPhotos);
          }
        } else {
          Navigator.pop(context);
        }
      },
    );
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    Wakelock.enable();
    _initCameraController();
  }

  @override
  void dispose() {
    _controller?.dispose();
    _faceDetector.close();
    WidgetsBinding.instance.removeObserver(this);
    Wakelock.disable();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // App state changed before we got the chance to initialize.
    final controller = _controller;
    if (controller == null || !controller.value.isInitialized) {
      return;
    }

    if (state == AppLifecycleState.inactive) {
      controller.dispose();
    } else if (state == AppLifecycleState.resumed) {
      _initCameraController();
    }
  }

  Future<void> _initCameraController() async {
    final cameras = await availableCameras();
    for (var i = 0; i < cameras.length; i++) {
      if (cameras[i].lensDirection == CameraLensDirection.front) {
        _camera = cameras[i];
        break;
      }
    }
    if (_camera == null) {
      log("Front camera not found!");
      return;
    }

    final controller = CameraController(
      _camera!,
      ResolutionPreset.high,
      enableAudio: false,
      imageFormatGroup: ImageFormatGroup.nv21,
    );
    _controller = controller;

    try {
      await controller.initialize();
      if (mounted) {
        setState(() {});
        controller.startImageStream((image) {
          _processImage(image);
        });
      }
    } on CameraException catch (e) {
      log("initializeCameraController failed", error: e);
      if (mounted) {
        Navigator.pop(context);
      }
    }
  }

  /// CameraImage
  void _processImage(CameraImage image) async {
    if (_livenessContext.isCompleted || _isProcessingImage) return;
    final inputImage = _inputImageFromCameraImage(image);
    if (inputImage == null || inputImage.metadata == null) {
      return;
    }
    _isProcessingImage = true;

    if (_needUpdateImageInfo) {
      final imageSize = inputImage.metadata!.size;
      final rotatiton = inputImage.metadata!.rotation;
      switch (rotatiton) {
        case InputImageRotation.rotation180deg:
        case InputImageRotation.rotation0deg:
          _imageWidth = imageSize.width;
          _imageHeight = imageSize.height;
          break;
        case InputImageRotation.rotation270deg:
        case InputImageRotation.rotation90deg:
          _imageWidth = imageSize.height;
          _imageHeight = imageSize.width;
          break;
      }
      _needUpdateImageInfo = false;
    }

    final faces = await _faceDetector.processImage(inputImage);
    if (faces.length > 1) {
      _onFaceError(_FaceError.multiFace);
      _livenessContext.reset();
    } else if (faces.isEmpty ||
        !DetectionUtils.isWholeFace(faces.first, _imageWidth, _imageHeight)) {
      _consecutiveEmptyFacesTimes++;
      if (_consecutiveEmptyFacesTimes >= 5) {
        _consecutiveEmptyFacesTimes = 0;
        _onFaceError(_FaceError.noFace);
        _livenessContext.reset();
      }
    } else {
      _consecutiveEmptyFacesTimes = 0;
      final face = faces.first;
      _onFaceFrame(_livenessContext.currentState);
      _livenessContext.handle(face);
    }
    _isProcessingImage = false;
  }

  void _onFaceError(_FaceError error) {
    _faceError = error;
    switch (error) {
      case _FaceError.noFace:
        _stateText.value = "No faces detected";
      case _FaceError.multiFace:
        _stateText.value = "Multiple faces detected";
    }
  }

  void _onFaceFrame(LivenessState state) {
    _faceError = null;
    if (state is FrontFaceState) {
      _stateText.value = "Please look at the camera";
    } else if (state is SideFaceState) {
      _stateText.value = "Please show your side face";
    } else if (state is SmileState) {
      _stateText.value = "Please smile";
    } else if (state is MouthOpenState) {
      _stateText.value = "Please open your mouth";
    }
  }

  final _orientations = {
    DeviceOrientation.portraitUp: 0,
    DeviceOrientation.landscapeLeft: 90,
    DeviceOrientation.portraitDown: 180,
    DeviceOrientation.landscapeRight: 270,
  };

  InputImage? _inputImageFromCameraImage(CameraImage image) {
    final controller = _controller;
    final camera = _camera;
    if (controller == null ||
        !controller.value.isInitialized ||
        camera == null) {
      return null;
    }

    // get image rotation
    // it is used in android to convert the InputImage from Dart to Java: https://github.com/flutter-ml/google_ml_kit_flutter/blob/master/packages/google_mlkit_commons/android/src/main/java/com/google_mlkit_commons/InputImageConverter.java
    // `rotation` is not used in iOS to convert the InputImage from Dart to Obj-C: https://github.com/flutter-ml/google_ml_kit_flutter/blob/master/packages/google_mlkit_commons/ios/Classes/MLKVisionImage%2BFlutterPlugin.m
    // in both platforms `rotation` and `camera.lensDirection` can be used to compensate `x` and `y` coordinates on a canvas: https://github.com/flutter-ml/google_ml_kit_flutter/blob/master/packages/example/lib/vision_detector_views/painters/coordinates_translator.dart
    final sensorOrientation = camera.sensorOrientation;
    // print(
    //     'lensDirection: ${camera.lensDirection}, sensorOrientation: $sensorOrientation, ${_controller?.value.deviceOrientation} ${_controller?.value.lockedCaptureOrientation} ${_controller?.value.isCaptureOrientationLocked}');
    var rotationCompensation =
        _orientations[controller.value.deviceOrientation];
    if (rotationCompensation == null) return null;
    if (camera.lensDirection == CameraLensDirection.front) {
      // front-facing
      rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
    } else {
      // back-facing
      rotationCompensation =
          (sensorOrientation - rotationCompensation + 360) % 360;
    }
    InputImageRotation? rotation =
        InputImageRotationValue.fromRawValue(rotationCompensation);
    if (rotation == null) return null;

    // get image format
    final format = InputImageFormatValue.fromRawValue(image.format.raw);
    // validate format depending on platform
    // only supported formats:
    // * nv21 for Android
    // * bgra8888 for iOS
    if (format == null || format != InputImageFormat.nv21) return null;

    // since format is constraint to nv21 or bgra8888, both only have one plane
    if (image.planes.length != 1) return null;
    final plane = image.planes.first;

    // compose InputImage using bytes
    return InputImage.fromBytes(
      bytes: plane.bytes,
      metadata: InputImageMetadata(
          size: Size(image.width.toDouble(), image.height.toDouble()),
          rotation: rotation,
          format: format,
          bytesPerRow: plane.bytesPerRow),
    );
  }

  Future<XFile?> _takePhoto() async {
    final controller = _controller;
    if (controller == null ||
        !controller.value.isInitialized ||
        controller.value.isTakingPicture) {
      return null;
    }
    try {
      return await controller.takePicture();
    } on CameraException catch (e, s) {
      log("take photo failed", error: e, stackTrace: s);
      return null;
    }
  }

  Widget _cameraPreview() {
    final CameraController? cameraController = _controller;
    if (cameraController == null || !cameraController.value.isInitialized) {
      return const SizedBox();
    } else {
      return SizedBox(
        child: AspectRatio(
          aspectRatio: 1,
          child: ClipOval(
            child: FittedBox(
              fit: BoxFit.cover,
              child: SizedBox(
                width: cameraController.value.previewSize?.height,
                height: cameraController.value.previewSize?.width,
                child: CameraPreview(cameraController),
              ),
            ),
          ),
        ),
      );
      // return CameraPreview(cameraController);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Liveness"),
        centerTitle: true,
        leading: IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back_ios_new_rounded)),
      ),
      body: SizedBox.expand(
        child: Column(
          children: [
            ValueListenableBuilder(
              valueListenable: _stateText,
              builder: (context, value, child) {
                return Text(
                  value,
                  style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: _faceError == null ? Colors.black : Colors.red),
                );
              },
            ),
            const SizedBox(height: 20),
            FractionallySizedBox(widthFactor: 0.72, child: _cameraPreview()),
          ],
        ),
      ),
    );
  }
}
