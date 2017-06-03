all:
	
	javac test_server.java
	
	chmod +x normal_web_server
clean:
	
	%(RM) *.class
