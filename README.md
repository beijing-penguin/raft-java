# raft-java
## 如果出现leader节点由于网络问题，出现被孤立的情况，则该leader节点被client访问时，基于日志不能复制到其他超过半数的节点，自动降级为follower