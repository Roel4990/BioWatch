# BioWatch Analysis API Contract

이 문서는 Galaxy Watch BioWatch 앱과 연구용 Random Forest FastAPI 서버 사이의 API 계약을 정의합니다. 분석 결과는 의료 진단이 아닌 연구용 참고 결과입니다.

## Local configuration

실제 서버 정보는 프로젝트 루트의 `.env`에만 작성하며 Git에 커밋하지 않습니다.

```dotenv
ANALYSIS_API_BASE_URL=http://192.168.0.10:8000
ANALYSIS_API_TOKEN=replace_with_local_token
```

- 워치와 서버 PC는 같은 네트워크에서 서로 통신할 수 있어야 합니다.
- `localhost`와 `127.0.0.1`은 워치에서 서버 PC를 가리키지 않으므로 사용하지 않습니다.
- 초기 개발 서버는 HTTP를 사용하지만 외부 네트워크 또는 운영 환경에서는 HTTPS를 사용해야 합니다.
- Android 앱에 포함된 Bearer 토큰은 APK에서 완전히 숨길 수 없으므로 연구용 접근 제어 수준으로만 사용합니다.

## Authentication

`/health`를 제외한 API는 다음 헤더를 사용합니다.

```http
Authorization: Bearer {token}
```

## Health

```http
GET /health
```

인증 없이 호출합니다.

```json
{
  "status": "ok",
  "model_loaded": true,
  "model_version": "rf_relative_z_experimental_v1"
}
```

## Model information

```http
GET /api/v1/model/info
```

인증 적용 여부는 서버 확인이 필요합니다.

응답에는 최소한 모델 버전, 피처 이름과 순서, 클래스 라벨, window 길이, 샘플링 주파수 요구사항을 포함해야 합니다.

## Calibration

```http
POST /api/v1/calibrations
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

Multipart fields:

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `subject_id` | string | yes | 익명 subject ID |
| `file` | CSV | yes | 최소 5분 정상 calibration 데이터 |

필수 CSV 컬럼:

```text
Timestamp,HR,PPG_GREEN,PPG_STAT,IS_OFFBODY
```

선택 CSV 컬럼:

```text
HRV_RMSSD,ACC_X,ACC_Y,ACC_Z
```

초기 버전은 CSV만 지원하며 JSON과 gzip은 지원하지 않습니다. 최대 업로드 크기는 5MB입니다.

```json
{
  "baseline_id": "bl_123456",
  "subject_id": "subject_01",
  "valid_window_count": 5,
  "status": "ready",
  "created_at": "2026-08-11T10:30:00+09:00"
}
```

## Prediction

```http
POST /api/v1/predictions
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

Multipart fields:

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `subject_id` | string | yes | calibration과 동일한 익명 subject ID |
| `baseline_id` | string | yes | calibration 응답에서 받은 기준 ID |
| `file` | CSV | yes | 최근 센서 데이터 |

Prediction 파일은 정상적으로 60초를 모두 수집해 전송합니다. 서버 전처리의 최소 허용 길이는 55초이며, 유효한 60초 window 또는 신호 품질이 부족하면 `unavailable`을 반환합니다.

`HRV_RMSSD`는 선택 컬럼입니다. 서버는 `Timestamp`와 `PPG_GREEN`으로 샘플링 주파수, pulse peak, RR interval 및 HRV 피처를 다시 계산하며 `HR`은 PPG 검출 심박수 검증에 사용합니다.

```json
{
  "result": "normal",
  "abnormal_probability": 0.18,
  "threshold": 0.5,
  "baseline_id": "bl_123456",
  "baseline_applied": true,
  "valid_window_count": 1,
  "model_version": "rf_relative_z_experimental_v1"
}
```

허용되는 `result` 값:

- `normal`
- `possible_abnormal`
- `unavailable`

`unavailable`인 경우 정상 또는 이상 결과를 추정하지 않으며 서버가 `reason`을 함께 반환하는 것을 권장합니다.

## Acute stress prediction

```http
POST /api/v1/stress/predictions
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

요청 필드는 `subject_id`, `baseline_id`, `file`이며 기존 Prediction API와 같습니다. 약 60초 CSV를 권장하고 최소 55초 이상이어야 합니다. 5분 CSV는 서버가 60초 단위로 나누어 가장 높은 확률을 반환합니다.

허용되는 `result` 값:

- `normal`
- `possible_acute_stress`
- `unavailable`

확률 필드명은 `acute_stress_probability`이고 판정 threshold는 `0.5`입니다. `medical_diagnosis`는 `false`이며 연구용 실험 결과로만 표시합니다.

## Model behavior

- 모델 버전: `rf_relative_z_experimental_v1`
- 모델: Random Forest
- 이상 가능성 threshold: `0.5`
- baseline은 subject별 평균과 표준편차 계산에 실제 사용됩니다.
- relative: `(현재값 - 개인 정상 평균) / 개인 정상 평균`
- z-score: `(현재값 - 개인 정상 평균) / 개인 정상 표준편차`
- `subject_id`와 `baseline_id`가 일치하지 않으면 `unavailable`입니다.

## Client timeouts

- connect timeout: 10초
- read/response timeout: 30초
- upload limit: 5MB

## Items requiring server confirmation

다음 항목은 후속 기능을 확장하기 전에 확인이 필요합니다.

1. `/api/v1/model/info`의 인증 필요 여부와 정확한 응답 스키마
2. `unavailable`의 HTTP 상태 코드와 `reason` 필드의 허용 값
3. 4xx/5xx 공통 오류 응답 스키마
4. calibration baseline의 만료·삭제·재생성 정책
5. 동일 subject의 기존 baseline이 있을 때 새 calibration 생성 동작
