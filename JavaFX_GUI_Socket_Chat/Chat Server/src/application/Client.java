package application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

// 서버와 한명의 클라이언트가 통신을 하기 위해서 필요한 기능들을 정의
public class Client {
	Socket socket;
	
	public Client(Socket socket) {
		this.socket = socket;
		receive();
	}
	
	// 반복적으로 클라이언트로부터 메시지를 전달 받는 메소드입니다.
	public void receive() {
		Runnable thread = new Runnable() {
			// 하나의 쓰레드가 어떤 모듈로 동작을 하는 지 정의
			@Override
			public void run() {
				try {
					while(true) {
						InputStream in = socket.getInputStream();				// 내용전달을 받도록 객체 생성
						byte[] buffer = new byte[512];							// 512byte만큼 전달 받을 수 있게 설정
						int length = in.read(buffer);
						while(length == -1) throw new IOException();			// 전송 오류 발생 시 오류 전송
						System.out.println("[메시지 수신 성공] "
								+ socket.getRemoteSocketAddress()				// 현재 접속한 주소정보를 출력
								+ " : " + Thread.currentThread().getName());	// 스레드 고유이름 값 출력
						String message = new String(buffer, 0, length, "UTF-8");
						
						// 클라이언트로부터 메시지를 받으면 다른 클라이언트로 전송
						for(Client client : Main.clients) {
							client.send(message);
						}
					}
				} catch (Exception e) {
					try {
						System.out.println("[메시지 수신 오류] "
								+ socket.getRemoteSocketAddress()
								+ " : " + Thread.currentThread().getName());
					} catch (Exception e2) {
						
					}
				}
				
			}
		};
		Main.threadPool.submit(thread);
	}
	
	// 클라이언트에게 메시지를 전송하는 메소드입니다.
	public void send(String message) {
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				try {
					// 메시지 전송
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8");
					out.write(buffer);	// 서버에서 클라이언트로 메시지 전송
					out.flush();
				} catch (Exception e) {
					try {
						// 오류 발생 시 서버에서 클라이언트로 접속불량 알림
						System.out.println("[메시지 송신 오류]"
								+ socket.getRemoteSocketAddress()
								+ " : " + Thread.currentThread().getName());
						// 오류 발생 시 클라이언트가 접속불량이라는 것을 서버에 알림
						Main.clients.remove(Client.this);
						socket.close();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		};
		Main.threadPool.submit(thread);
	}

}
