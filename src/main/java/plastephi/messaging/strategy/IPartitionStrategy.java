package plastephi.messaging.strategy;

public interface IPartitionStrategy {
    int apply(int partitions, long value);

}
