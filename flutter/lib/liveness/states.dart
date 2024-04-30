import 'dart:developer';

import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';
import 'package:liveness/liveness/utils.dart';

abstract class LivenessState {
  void handle(LivenessContext context, Face face);
}

abstract class FramesLimitState implements LivenessState {
  final int limit;
  int _currentFrame = 0;

  FramesLimitState(this.limit);

  @override
  void handle(LivenessContext context, Face face) {
    if (isPass(face)) {
      _currentFrame++;
      if (_currentFrame > limit) {
        _currentFrame = 0;
        context._next();
      }
    } else {
      _currentFrame = 0;
    }
  }

  bool isPass(Face face);
}

class FrontFaceState extends FramesLimitState {
  FrontFaceState(super.limit);

  @override
  bool isPass(Face face) {
    return DetectionUtils.isFrontFace(face);
  }
}

class SideFaceState extends FramesLimitState {
  SideFaceState(super.limit);

  @override
  bool isPass(Face face) {
    return DetectionUtils.isSideFace(face);
  }
}

class SmileState extends FramesLimitState {
  SmileState(super.limit);

  @override
  bool isPass(Face face) {
    return DetectionUtils.isSmiling(face);
  }
}

class MouthOpenState extends FramesLimitState {
  MouthOpenState(super.limit);

  @override
  bool isPass(Face face) {
    return DetectionUtils.isMouthOpen(face);
  }
}

class _CompleteState extends LivenessState {
  @override
  void handle(LivenessContext context, Face face) {
    // completed
    log("completed");
  }
}

class LivenessContext {
  final List<LivenessState> _states;
  final void Function(
      {required void Function() next,
      required void Function() retry}) stateChangeCallback;
  final void Function(int photoCount) onCompleted;

  LivenessContext(
      {required List<LivenessState> states,
      required this.stateChangeCallback,
      required this.onCompleted}) : _states = List.of(states) {
    // 哨兵
    _states.add(_CompleteState());
  }

  int _stateIndex = 0;
  bool _transitioning = false;

  void handle(Face face) {
    if (_transitioning) return;
    currentState.handle(this, face);
  }

  void _next() {
    if (_transitioning) {
      return;
    }
    _transitioning = true;
    stateChangeCallback(
        next: () {
          // 二次确认状态转移，await 过程中状态可能已经转移，比如外部调用了resetState
          if (_transitioning) {
            _transitioning = false;
            _stateIndex++;
            if (_stateIndex == _states.length - 1) {
              onCompleted(_states.length - 1);
            }
          }
        },
        retry: () => _transitioning = false);
  }

  void reset() {
    _transitioning = false;
    _stateIndex = 0;
  }

  LivenessState get currentState => _states[_stateIndex];

  bool get isCompleted => _stateIndex == _states.length - 1;
}
