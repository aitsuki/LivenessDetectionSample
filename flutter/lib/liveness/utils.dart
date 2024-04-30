import 'dart:math';

import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';

class DetectionUtils {
  DetectionUtils._();

  static const double _yawThreshold = 12;
  static const double _pitchThreshold = 12;
  static const double _rollThreshold = 12;
  static const double _sideFaceYawThreshold = 20;

  static bool isWholeFace(Face face, double imageWidth, double imageHeight) {
    final boundingBox = face.boundingBox;
    final coutours = face.contours[FaceContourType.face];
    if (coutours == null) return false;
    final top = boundingBox.top;
    final bottom = boundingBox.bottom;
    final left = coutours.points[27].x;
    final right = coutours.points[9].x;
    return top > 0 && bottom < imageHeight && left > 0 && right < imageHeight;
  }

  static bool isFrontFace(Face face) {
    final yaw = face.headEulerAngleY; // 左右摇头角度
    final pitch = face.headEulerAngleX; // 上下点头角度
    final roll = face.headEulerAngleZ; // 旋转角度
    if (yaw == null || pitch == null || roll == null) {
      return false;
    }
    return yaw < _yawThreshold &&
        yaw > -_yawThreshold &&
        pitch < _pitchThreshold &&
        pitch > -_pitchThreshold &&
        roll < _rollThreshold &&
        roll > -_rollThreshold;
  }

  static bool isSideFace(Face face) {
    final yaw = face.headEulerAngleY; // 左右摇头角度
    final pitch = face.headEulerAngleX; // 上下点头角度
    final roll = face.headEulerAngleZ; // 旋转角度
    if (yaw == null || pitch == null || roll == null) {
      return false;
    }
    return (yaw > _sideFaceYawThreshold || yaw < -_sideFaceYawThreshold) &&
        pitch < _pitchThreshold &&
        pitch > -_pitchThreshold &&
        roll < _rollThreshold &&
        roll > -_rollThreshold;
  }

  static bool isSmiling(Face face) {
    if (!isFrontFace(face)) return false;
    final smile = face.smilingProbability;
    if (smile == null) return false;
    return smile > 0.6;
  }

  static bool isMouthOpen(Face face) {
    if (!isFrontFace(face)) return false;
    final left = face.landmarks[FaceLandmarkType.leftMouth]?.position;
    final right = face.landmarks[FaceLandmarkType.rightMouth]?.position;
    final bottom = face.landmarks[FaceLandmarkType.bottomMouth]?.position;
    if (left == null || right == null || bottom == null) {
      return false;
    }

    int a2 = right.squaredDistanceTo(bottom);
    int b2 = left.squaredDistanceTo(bottom);
    int c2 = left.squaredDistanceTo(right);

    double a = sqrt(a2);
    double b = sqrt(b2);
    double gamma = acos((a2 + b2 - c2) / (2 * a * b));
    double gammaDeg = gamma * 180 / pi;
    return gammaDeg < 115;
  }
}
