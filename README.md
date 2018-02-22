## raft选举：投票超过半数以上的node为领导，且保证集群中始终在一任期中只会存在一位领导
## raft日志复制：日志信息和数据存储在关系型数据库中，根据dataIndex做日志压缩，同时也保证了性能和方便性,存储下来的数据dataIndex严格递增
## raft集群虚拟节点：增加单节点中的虚拟节点，hash分配数据到虚拟节点上，多线程处理来保证性能
## raft动画图解  http://thesecretlivesofdata.com/raft/