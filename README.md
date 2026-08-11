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
- 센서 데이터
  - `ACTIVITY_RECOGNITION`
- API 35 이하
  - `BODY_SENSORS`
  - API 33~35: `BODY_SENSORS_BACKGROUND`
- API 36 이상
  - `READ_HEART_RATE`
  - `READ_HEALTH_DATA_IN_BACKGROUND`
- Foreground Service
  - `FOREGROUND_SERVICE`
  - `FOREGROUND_SERVICE_HEALTH`
- Samsung raw sensor data
  - `READ_ADDITIONAL_HEALTH_DATA`

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

## 센서 데이터 저장

메인 화면의 `BioWatch` 제목을 빠르게 세 번 누르고 관리자 PIN을 입력해 전용 화면으로 이동한 뒤 익명 subject ID, 상태와 수집 목적을 설정하고 `수집 시작`을 누릅니다. 일반 심박수 화면에서는 CSV 파일을 생성하지 않습니다. 상태는 `normal`, `unknown`만 제공하며 의료 장비로 확인되지 않은 부정맥 라벨은 생성하지 않습니다.

수집 목적은 다음과 같습니다.

- `calibration`: 개인 정상 기준 생성용
- `evaluation`: 기준 생성 이후 평가용

수집 중에는 경과 시간, PPG 샘플 수, 센서 timestamp 기준 예상 샘플링 주파수를 표시합니다. `수집 중단 및 저장`을 누르면 UTF-8 CSV와 JSON 메타데이터를 함께 생성합니다.

```text
Android/data/com.example.biowatch/files/Documents/BioWatch/
```

파일명 형식은 다음과 같습니다.

```text
SamsungWatch_{subjectId}_{state}_{purpose}_{startTimestamp}.csv
SamsungWatch_{subjectId}_{state}_{purpose}_{startTimestamp}.json
```

CSV 컬럼은 다음과 같습니다.

```text
Timestamp,HR,PPG_GREEN,PPG_STAT,IS_OFFBODY,ACC_X,ACC_Y,ACC_Z
```

- `Timestamp`: PPG 센서 이벤트 timestamp를 워치 시간대가 포함된 ISO 8601 형식으로 변환한 값
- `HR`: 해당 PPG 행 시점의 마지막 유효 bpm. 아직 유효한 값이 없으면 빈 값
- `PPG_GREEN`: Samsung Health Sensor SDK가 제공한 green PPG 원본 정수값
- `PPG_STAT`: SDK의 green PPG 상태 코드 원본값
- `IS_OFFBODY`: `0=착용 또는 확인 불가`, `1=미착용`
- `ACC_X`, `ACC_Y`, `ACC_Z`: 해당 PPG 행 시점의 마지막 가속도 원본 정수값. 미지원 또는 아직 수신 전이면 빈 값

Samsung Health Sensor SDK 공개 API는 PPG 및 가속도 정수값의 물리 단위를 명시하지 않으므로 정규화나 단위 변환 없이 원본값을 저장합니다. 앱은 값을 보간하거나 가짜 PPG를 생성하지 않습니다.

실제 Galaxy Watch SM-L330에서 SDK 1.4.1의 `PPG_CONTINUOUS`, `HEART_RATE_CONTINUOUS`, `ACCELEROMETER_CONTINUOUS` 지원을 확인했습니다. 다른 모델에서는 앱 실행 시 tracker 지원 여부를 다시 확인하며 raw PPG가 지원되지 않으면 수집을 시작하지 않습니다.

정상 기준 데이터는 앉아서 5분 이상 안정을 취한 뒤 시계를 손목에 밀착하고, 말하거나 걷거나 운동하지 않은 상태에서 5~10분 수집하는 것을 권장합니다. calibration과 evaluation에는 동일한 센서 샘플을 재사용하지 않습니다.
