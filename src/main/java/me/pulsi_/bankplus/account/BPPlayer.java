package me.pulsi_.bankplus.account;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.pulsi_.bankplus.bankSystem.Bank;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class BPPlayer {

    private final Player player;

    private Bank openedBank;
    private ScheduledTask bankUpdatingTask, closingTask;

    // Values to check if the player is doing a deposit or withdraw through chat.
    private boolean depositing, withdrawing;

    public BPPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Bank getOpenedBank() {
        return openedBank;
    }

    public ScheduledTask getBankUpdatingTask() {
        return bankUpdatingTask;
    }

    public ScheduledTask getClosingTask() {
        return closingTask;
    }

    public boolean isDepositing() {
        return depositing;
    }

    public boolean isWithdrawing() {
        return withdrawing;
    }

    public void setOpenedBank(Bank openedBank) {
        this.openedBank = openedBank;
    }

    public void setBankUpdatingTask(ScheduledTask bankUpdatingTask) {
        this.bankUpdatingTask = bankUpdatingTask;
    }

    public void setClosingTask(ScheduledTask closingTask) {
        this.closingTask = closingTask;
    }

    public void setDepositing(boolean depositing) {
        this.depositing = depositing;
    }

    public void setWithdrawing(boolean withdrawing) {
        this.withdrawing = withdrawing;
    }
}