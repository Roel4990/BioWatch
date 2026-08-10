# BioWatch

Galaxy Watch의 Samsung Health Sensor SDK를 사용해 심박수를 측정하는 Wear OS 앱입니다.

> 이 앱은 연구 및 피트니스 목적의 프로토타입이며 의료 진단이나 치료 목적으로 사용할 수 없습니다.

## 개발 환경

- Galaxy Watch4 이상
- Wear OS Powered by Samsung
- Samsung Health Sensor SDK 1.4.1
- Android Studio 내장 JDK 또는 JDK 17 이상

Samsung SDK AAR는 라이선스 제한으로 저장소에 포함하지 않습니다. Samsung Developer에서 SDK를 내려받은 후 다음 경로에 직접 추가해야 합니다.

```text
app/libs/samsung-health-sensor-api-1.4.1.aar
```

## 워치 설정

개발 빌드는 Samsung Health Platform 개발자 모드가 활성화된 실제 Galaxy Watch에서 테스트해야 합니다. Samsung Health Sensor SDK는 에뮬레이터를 지원하지 않습니다.

## 권한

앱은 Android 및 Wear OS 버전에 따라 다음 권한을 요청합니다.

- API 33 이상
  - `POST_NOTIFICATIONS`
- API 35 이하
  - `BODY_SENSORS`
  - API 33~35: `BODY_SENSORS_BACKGROUND`
- API 36 이상
  - `READ_HEART_RATE`
  - `READ_HEALTH_DATA_IN_BACKGROUND`
- Foreground Service
  - `FOREGROUND_SERVICE`
  - `FOREGROUND_SERVICE_HEALTH`

전면 심박수 권한과 백그라운드 권한은 별도로 요청합니다. 백그라운드 권한을 거부하면 Foreground Service를 시작하지 않습니다.

## 백그라운드 측정

심박수 측정을 시작하면 `health` 타입 Foreground Service와 지속 알림이 생성됩니다. 측정은 앱이 백그라운드로 이동하거나 워치 화면이 꺼져도 유지됩니다.

측정을 중단하는 방법은 다음과 같습니다.

- 앱 화면에서 `측정 중단` 선택
- 지속 알림에서 `측정 중단` 선택

Service 종료 시 심박수 tracker, 착용 감지 센서 리스너, Samsung Health Platform 연결을 모두 해제합니다.

앱 강제 종료, 프로세스 강제 종료 또는 워치 재부팅 이후 자동 재시작은 지원하지 않습니다.

## 실행 및 테스트

1. 워치를 Android Studio 실행 기기로 연결합니다.
2. 앱을 실행하고 전면 및 백그라운드 Health 권한을 허용합니다.
3. 앱에서 심박수가 표시되는지 확인합니다.
4. 홈 화면으로 이동하거나 워치 화면을 끕니다.
5. 지속 알림이 유지되는지 확인합니다.
6. 5분 이상 기다린 후 앱으로 돌아와 심박수 측정이 이어지는지 확인합니다.
7. 앱 또는 알림에서 측정을 중단하고 알림과 센서 연결이 종료되는지 확인합니다.

Logcat에서는 다음 태그로 상태를 확인할 수 있습니다.

```text
HeartRateService
SamsungHealthSensor
HealthRepository
```
