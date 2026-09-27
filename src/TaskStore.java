import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存任务看板：增删改查，完成任务会奖励宠物经验。
 * 数据仅存于内存，重启后清空（演示用）。
 */
public class TaskStore {

    private static final int REWARD_EXP = 20;

    public static class Task {
        final long id;
        volatile String title;
        volatile boolean done;
        final String createdAt;

        Task(long id, String title) {
            this.id = id;
            this.title = title;
            this.createdAt = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        }

        String toJson() {
            return "{\"id\":" + id
                    + ",\"title\":\"" + Json.esc(title) + "\""
                    + ",\"done\":" + done
                    + ",\"createdAt\":\"" + createdAt + "\"}";
        }
    }

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();
    private final Pet pet;

    public TaskStore(Pet pet) {
        this.pet = pet;
    }

    /** 新增任务 */
    public synchronized Task add(String title) {
        Task t = new Task(seq.incrementAndGet(), title);
        tasks.put(t.id, t);
        return t;
    }

    /** 切换完成状态；变为完成时给宠物加经验，返回反馈文案 */
    public synchronized String toggle(long id) {
        Task t = tasks.get(id);
        if (t == null) return null;
        t.done = !t.done;
        if (t.done) {
            String msg = "任务完成！宠物获得 " + REWARD_EXP + " 点经验！";
            if (pet.gainExp(REWARD_EXP)) {
                msg += " 宠物升级啦！";
            }
            return msg;
        }
        return "已取消完成（经验不退哦）";
    }

    /** 删除任务 */
    public synchronized boolean delete(long id) {
        return tasks.remove(id) != null;
    }

    /** 全部任务（按 id 升序） */
    public synchronized List<String> list() {
        return tasks.values().stream()
                .sorted(Comparator.comparingLong(t -> t.id))
                .map(Task::toJson)
                .collect(Collectors.toList());
    }

    /** 任务统计 */
    public synchronized String statsJson() {
        long total = tasks.size();
        long done = tasks.values().stream().filter(t -> t.done).count();
        long undone = total - done;
        int rate = total == 0 ? 0 : (int) (done * 100 / total);
        return "{\"total\":" + total
                + ",\"done\":" + done
                + ",\"undone\":" + undone
                + ",\"completionRate\":" + rate + "}";
    }
}
