# UDP_With_Selective_Repeat
Comp_6461_Assignment-3

For Router:

./router --port=3333 --drop-rate=0.0 --max-delay=0ms --seed=1
./router --port=3333 --drop-rate=0.3 --max-delay=10ms --seed=1
./router --port=3333 --drop-rate=0.2
./router --port=3333 --max-delay=10ms


For Server :

httpfs -v -p 8080

httpfs -v -p 8080 -d <Path>
httpfs -v -p 8080 -d /Users/ashwinraghunath/Documents/Fall_2022/COMP_6461_CN/Assignments/UDP_With_Selective_Repeat/Demo

For Client :

httpc http://localhost:8080/get/
httpc http://localhost:8080/post/PostFile1.txt -v -d '{"Assignment":3, "Course" : 6461}'
httpc http://localhost:8080/post/PostFile1.txt -v -h Content-Type:application/json -d '{"Assignment":3, "Course" : 6461}'
httpc http://localhost:8080/post/PostFile1.txt -d '{"Assignment":3, "Course" : 6461}'
httpc http://localhost:8080/post/JSONTestFile.json -d '{"Assignment":3}'
httpc http://localhost:8080/get/GetFile.txt
httpc http://localhost:8080/get/PostFile1.txt
httpc http://localhost:8080/post/PostFile2.txt -d Hello World

httpc http://localhost:8080/get/griffin.txt
httpc http://localhost:8080/get/demo_file.txt


