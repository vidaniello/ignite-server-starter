package com.github.vidaniello.igniteserver;

public class MainLoader {

	public static void main(String[] args) {
		new Thread(new MainThread(args)).start();
	}

}
