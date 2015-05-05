# MicroBlogCrawler

This project contains three part: twitter analysis, weibo analysis and frontpage

## project directory
#### sinaMaven
This part is for sina weibo crawler and analysis. 

#### twitterMaven
This part is for twitter topic time series analysis. Twitter does not support retweet network building because the retweet link can only spread from the original source.

#### frontForweibo
This part is web interface for weibo. D3.js and javascript are used to show the retweet network.

## How to use
### sina analysis
1. install neo4j database on your computer
2. stall intellij to edit the project(other ide is OK)
3. first inspect the topic in weibo. The start time and end time are needed. And make sure the total amounts(too many tweet may lead to very long time)
4. crawl the topic into the database. Change the topic name in /SinaMaven/src/main/java/m/sina/sinaTopic.java. Change the start time and end time in method testOnTopic() under /SinaMaven/src/main/java/m/sina/sinaTopic.java. Then run the main method.
5. centrality analysis methods are located in /SinaMaven/src/main/java/m/sina/Centrality.java. Degree and time series can be output to a file in /SinaMaven/src/main/java/m/sina/mSinaAnalysis.java
6. web interface. Start the neo4j database server for specific database. Use 'neo4j -p file/path/to/your/location'. Then open the web_interface.html.

### twitter analysis
1. change the topic name in the main method in MicroBlogCrawler/twitterMaven/src/main/java/twitterTopic.java
2. run the main method


 