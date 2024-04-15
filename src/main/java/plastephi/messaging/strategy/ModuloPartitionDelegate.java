package plastephi.messaging.strategy;

public class ModuloPartitionDelegate implements IPartitionStrategy {
    @Override
    public int apply(int partitions, long value) {
        return (int) (value % partitions);
    }
}
