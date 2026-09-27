import java.time.Duration;
import java.time.Instant;

/**
 * 卡通宠物模型：饱食度与心情随时间衰减（懒计算），
 * 喂食恢复饱食度，玩耍提升心情并获得经验，经验满升级。
 */
public class Pet {

    private String name = "皮卡球";
    private int level = 1;
    private int exp = 0;       // 当前经验
    private int satiety = 80;  // 饱食度 0-100
    private int mood = 80;     // 心情 0-100
    private int feedCount = 0;
    private int playCount = 0;
    private Instant lastTick = Instant.now();

    /** 随时间衰减：每分钟饱食度 -1、每两分钟心情 -1（有下限） */
    private synchronized void tick() {
        long minutes = Duration.between(lastTick, Instant.now()).toMinutes();
        if (minutes <= 0) return;
        satiety = Math.max(0, satiety - (int) Math.min(minutes, 60));
        mood = Math.max(20, mood - (int) Math.min(minutes / 2, 40));
        lastTick = Instant.now();
    }

    /** 喂食：不同食物恢复量不同，返回反馈文案 */
    public synchronized String feed(String food) {
        tick();
        feedCount++;
        String f = food == null ? "" : food.trim();
        int gain;
        String msg;
        if (f.contains("蛋糕") || f.toLowerCase().contains("cake")) {
            gain = 35;
            msg = name + "吃到了蛋糕，幸福得冒泡泡！";
        } else if (f.contains("苹果") || f.toLowerCase().contains("apple")) {
            gain = 25;
            msg = name + "嘎吱嘎吱地啃苹果～";
        } else {
            gain = 15;
            msg = name + "把" + (f.isEmpty() ? "零食" : f) + "吃得一干二净！";
        }
        satiety = Math.min(100, satiety + gain);
        mood = Math.min(100, mood + 5);
        return msg;
    }

    /** 玩耍：心情提升、消耗饱食度并获得经验，返回反馈文案 */
    public synchronized String play() {
        tick();
        playCount++;
        mood = Math.min(100, mood + 15);
        satiety = Math.max(0, satiety - 8);
        String msg = name + "玩得满地打滚，开心到起飞！";
        if (gainExp(30)) {
            msg += " 升级啦！现在是 Lv." + level + "！";
        }
        return msg;
    }

    /** 完成任务奖励经验（由 TaskStore 调用），返回是否升级 */
    public synchronized boolean gainExp(int amount) {
        tick();
        boolean up = addExp(amount);
        if (up) {
            mood = Math.min(100, mood + 10);
        }
        return up;
    }

    /** 经验累加，每级需求为 level*100 */
    private boolean addExp(int amount) {
        exp += amount;
        boolean up = false;
        while (exp >= level * 100) {
            exp -= level * 100;
            level++;
            up = true;
        }
        return up;
    }

    /** 表情随状态变化，供前端展示 */
    private synchronized String face() {
        if (satiety < 25) return "😵";
        if (mood >= 80 && satiety >= 60) return "🥳";
        if (mood >= 50) return "😊";
        return "😭";
    }

    public synchronized String toJson() {
        tick();
        String state;
        if (satiety < 25) state = "饿晕了";
        else if (mood < 50) state = "有点丧";
        else if (mood >= 80) state = "超开心";
        else state = "还不错";
        return "{\"name\":\"" + Json.esc(name) + "\","
                + "\"level\":" + level + ","
                + "\"exp\":" + exp + ","
                + "\"expMax\":" + level * 100 + ","
                + "\"satiety\":" + satiety + ","
                + "\"mood\":" + mood + ","
                + "\"face\":\"" + face() + "\","
                + "\"state\":\"" + state + "\","
                + "\"feedCount\":" + feedCount + ","
                + "\"playCount\":" + playCount + "}";
    }
}
