# Chatka

### 개발 목표
- 대규모 인원이 참여하는 실시간 채팅 모듈 개발
- 높은 데이터 처리량 및 안정성 확보

### 채팅 서버 특징
- WebSocket을 사용하여 서버와 클라이언트 간의 실시간 양방향 메시징
- 서버 내에서는 Akka toolkit을 이용하여 고속으로 병렬 처리
- Redis Pub/Sub 기능을 활용하여 채팅방에 접속한 유저들에게 입력된 메시지를 브로드캐스팅

### 고려  
주기적으로 서버에서 Heartbeat 신호를 발생하여 클라이언트와의 WebSocket 연결을 안정적으로 유지  
[application.conf](https://github.com/uomaep/Chatka/blob/main/src/main/resources/application.conf)의 141번 라인 http 블록 설정 확인

### 발생한 문제
부하테스트 중 WebSocket 연결 수가 많아졌을 때 연결 오류가 생겼습니다.  
WebSocket 연결은 TCP 소켓을 사용하고, TCP 소켓은 파일 디스크립터(FD)를 소비하기 때문에, 열 수 있는 파일의 개수(ulimit -n)를 충분히 늘려줘야 한다는 점을 알았습니다.(해결)

### Akka toolkit 특징
<img width="382" alt="스크린샷 2025-04-03 오후 11 13 43" src="https://github.com/user-attachments/assets/46f28f54-08e7-40c5-b361-102e6adfa450" />

- actor
  - 각각의 actor는 내부에 상태와 행동을 가지며, mailbox라는 queue가 할당된다.
  - actor 간에 서로 메시지를 주고받으며 수신한 메시지에 대해 각각 정의된 행동을 한 후 다음 actor로 메시지를 보낸다.
  - 메시지 패싱은 비동기적으로 이루어지기 때문에 메시지를 보낸 actor는 곧바로 자신의 mailbox에서 다음 메시지를 수신하여 일을 한다.

### 아키텍처
<img width="397" alt="스크린샷 2025-04-03 오후 11 00 03" src="https://github.com/user-attachments/assets/1c4f47ae-10d6-45c2-8adc-72b17f07f7d3" />

#### 채팅 서버의 Actor 시스템 구성
- ChatSupervisor
  - JVM에 단 하나만 존재하는 가장 최상위 Actor
  - 외부에서 유입되는 메시지를 그에 대응하는 actor로 패싱
- ChatRoomActor
  - 각 채팅방마다 생성되는 Actor
  - Redis의 pub/sub과 연계하여 채팅방에 접속하고 있는 유저 Actor로 브로드캐스팅
- UserActor
  - 한명의 유저의 WebSocket 연결
  - 메시지를 수신하면 소속 ChatRoomActor로 메시지 패싱
  - ChatRoomActor의 브로드캐스트를 받으면 WebSocket을 통해 유저에게 메시지 전달
 
### 부하 테스트
- 성능 테스트 도구: k6 (javascript 시나리오 테스트)
  - 가상 유저를 접속시키고, 가상 채팅메시지를 채팅방에 발송
  - 모든 가상 유저가 채팅을 다시 전송받을 때까지의 시간을 체크
  - 가상 유저와 채팅 수를 점진적으로 증가시키며 테스트
- 모니터링: influxDB + telegraf

|                  | Connection |
| ---------------- | ---------- |
| 10pub / 1sub     | 44.64ms    |
| 100pub / 5sub    | 59.06ms    |
| 1000pub / 10sub  | 77.67ms    |
| 1000pub / 50sub  | 91.07ms    |
| 1500pub / 100sub | 155.56ms   |

[cpu 사용량]  
<img width="353" alt="스크린샷 2025-04-03 오후 11 26 47" src="https://github.com/user-attachments/assets/ce6a0c38-56c7-47fc-ad42-3b8baf0f1628" />

*[received packet bytes]*  
<img width="280" alt="스크린샷 2025-04-03 오후 11 27 04" src="https://github.com/user-attachments/assets/877a9e86-c5c9-4c0f-b5e5-458c1f4d725c" />

*[sent packet bytes]*  
<img width="270" alt="스크린샷 2025-04-03 오후 11 27 17" src="https://github.com/user-attachments/assets/6482c09b-5c84-4c30-b9ba-0a53c0854a64" />
