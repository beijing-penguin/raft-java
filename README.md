# raft-java 状态：未完成
## ①raft选举根据投票超过半数以上才能成功，则能保证集群中始终在一任期中只会存在一位领导
## ②raft日志复制中，数据存储和日志同步，可以用保存在同一个数据库（如嵌入式H2数据库）中，日后便可以压缩日志，来提高数据库查询性能，和日志sync也不会受到影响，因为在数据持久化时，存储下来的数据dataIndex严格递增。数据压缩后，则也能知道数据被压缩到的位置。
## ③本raft-java项目相对于raft论文，代码实现时略有改动，暂时没用到候选人角色
##
## ④程序使用说明，配置好config.properties，启动StartServer.java即可。
## ⑤raft动画图解  http://thesecretlivesofdata.com/raft/