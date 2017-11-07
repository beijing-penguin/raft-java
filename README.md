# raft-java
## ①如果出现leader节点由于网络问题，出现被孤立的情况，则该leader节点被client访问时，基于日志不能复制到其他超过半数的节点，自动降级为follower该被孤立的leader如果接受到来自其他leaderNode的ping，则根据数据索引大于该leaderIndex也自动降级为follower
## ②当raft集群leader由于内部网络波动问题，连不上follower期间，follower自动检测并发起选举，主动发起选举的节点不接受来自client或者leader任何数据请求，即不保存任何数据，也不会做出任何相应，直到该raft整个集群中有leader被选举出来。