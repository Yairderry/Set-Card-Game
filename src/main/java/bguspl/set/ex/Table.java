package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 * @inv slotLocks.size() == env.config.tableSize
 */
public class Table {

    /**
     * The game environment object.
     */
    protected final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * Signifies when players are allowed to place tokens on the table.
     */
    protected volatile boolean tableReady = false;

    /**
     * ReadWrite lock for each slot of the table.
     */
    protected ReadWriteLock[] slotLocks;

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;

        this.slotLocks = new ReadWriteLock[slotToCard.length];
        for (int i = 0; i < slotLocks.length; i++)
            slotLocks[i] = new ReentrantReadWriteLock();
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // Display changes on ui
        env.ui.placeCard(card, slot);

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // Display changes on ui
        env.ui.removeTokens(slot);
        env.ui.removeCard(slot);

        cardToSlot[slotToCard[slot]] = null;
        slotToCard[slot] = null;

    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        env.ui.placeToken(player, slot);
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        env.ui.removeToken(player, slot);
        return true;
    }

    /**
     * Removes all tokens from the grid slots.
     */
    public void removeAllTokens(){
        env.ui.removeTokens();
    }

    /**
     * Functions for locking/unlocking slots.
     */
    public void lockSlot(int slot, boolean write){
        if (write)
            slotLocks[slot].writeLock().lock();
        else
            slotLocks[slot].readLock().lock();
    }

    public void unlockSlot(int slot, boolean write){
        if (write)
            slotLocks[slot].writeLock().unlock();
        else
            slotLocks[slot].readLock().unlock();
    }

    public void lockSlots(Integer[] slots, boolean write){
        for (Integer slot : slots)
            lockSlot(slot, write);
    }

    public void unlockSlots(Integer[] slots, boolean write){
        for (int i = slots.length-1; i >= 0; i--)
            unlockSlot(slots[i], write);
    }

    public void lockAllSlots(boolean write){
        for (int i = 0; i < slotLocks.length; i++)
            lockSlot(i, write);
    }

    public void unlockAllSlots(boolean write){
        for (int i = slotLocks.length-1; i >= 0; i--)
            unlockSlot(i, write);
    }


}
