#��Ŀ����裺

## ����Ŀ������

git@gitlab.sunlights.me:sunlights/p2pspay-dev-v2.git
	
## ����������

jdk1.6  play-1.2.7

## ��������jar����

* ����play-1.2.7\framework\lib������jar��
* lib������jar��
* play-1.2.7\framework\play-1.2.7.jar��

## �������в���

1. eclipse�У�
	
	cmd
	
	cd project path
	
	play eclipsify

	����java application
	
	sp2ponline-dev-v2��Ŀ����
	name:sp2ponline-dev-v2
	project:sp2ponline-dev-v2
	main class:play.server.Server
	arguments: -Xms512m -Xmx512m -XX:PermSize=512m -XX:MaxPermSize=512m -Xms512m -Xmx512m -XX:PermSize=126m -XX:MaxPermSize=126m -Xdebug -Dplay.debug=yes -Dplay.id= -Dapplication.path="${project_loc:sp2ponline-dev-v2}" -Djava.endorsed.dirs="C:\Program Files\play-1.2.7\framework/endorsed" -javaagent:"C:\Program Files\play-1.2.7\framework/play-1.2.7.jar"
	
	p2pspay-dev-v2��Ŀ���ã�
	name:p2pspay-dev-v2
	project:p2pspay-dev-v2
	main class:play.server.Server
	arguments:	-Xdebug  -Dplay.id= -Dapplication.path="${project_loc:p2pspay-dev-v2}" -Djava.endorsed.dirs="C:\Program Files\play-1.2.7/framework/endorsed" -javaagent:"C:\Program Files\play-1.2.7/framework/play-1.2.7.jar"

	
2. IDEA��	��

	 ����idea����Ŀ��play idealize
	
	 Edit configurations
	 
	 add Application
	 
		```Main Class:play.server.Server```
		
		```VM options:-Dapplication.path="."```
	 
	 
	 �����ְ汾����  VerifyError...XXX   ʹ�� play clean ɾ��tmp�ļ�

## ����

1. ����:play run

2. dev:play run --%dev

3. test:play run --%test

## ���

1. ����:play war -o myapp.war

2. dev:play war -o myapp.war --%%dev

3. test:play war -o myapp.war --%%test



        
        
		
		
		