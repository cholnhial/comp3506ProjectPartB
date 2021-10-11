public class SecurityDB extends SecurityDBBase {
    private SecurityHashMap hashMap;

    /**
     * Creates an empty hashtable and a variable to count non-empty elements.
     *
     * @param numPlanes             number of planes per day
     * @param numPassengersPerPlane number of passengers per plane
     */
    public SecurityDB(int numPlanes, int numPassengersPerPlane) {
        super(numPlanes, numPassengersPerPlane);

        int capacity = numPlanes * numPassengersPerPlane;
        // HashMap capacity needs to be set to a prime number
        capacity = getNextPrimeFrom(capacity);
        hashMap = new SecurityHashMap(capacity, MAX_CAPACITY);
    }

    /* Implement all the necessary methods here */

    /*
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        REMOVE THE MAIN FUNCTION BEFORE SUBMITTING TO THE AUTOGRADER
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        The following main function is provided for simple debugging only

        Note: to enable assertions, you need to add the "-ea" flag to the
        VM options of SecurityDB's run configuration
     */
    /*public static void main(String[] args) {
        SecurityDB db = new SecurityDB(3, 2);

        // add
        db.addPassenger("Rob Bekker", "Asb23f");
        db.addPassenger("Kira Adams", "MKSD23");
        db.addPassenger("Kira Adams", "MKSD24");
        assert db.contains("Asb23f");

        // count
        assert db.count() == 3;

        // del
        db.remove("MKSD23");
        assert !db.contains("MKSD23");
        assert db.contains("Asb23f");

        // hashcodes
        assert db.calculateHashCode("Asb23f") == 1717;

        // suspicious
        db = new SecurityDB(3, 2);
        db.addPassenger("Rob Bekker", "Asb23f");
        db.addPassenger("Robert Bekker", "Asb23f");
        // Should print a warning to stderr
    }*/

    @Override
    public int calculateHashCode(String key) {
        int sum =  0;
        for(int n = 1; n <= key.length(); n++) {
            sum += getHashCodeSum(n, key);
        }

        return sum;
    }

    private int getHashCodeSum(int end, String key) {
        int sum = 1;
        for (int i = 0; i < end; i++) {
            sum += key.charAt(i);
        }

        return sum;
    }

    @Override
    public int size() {
        return hashMap.size();
    }

    @Override
    public String get(String passportId) {
        return hashMap.get(calculateHashCode(passportId));
    }

    @Override
    public boolean remove(String passportId) {
        if (hashMap.remove(calculateHashCode(passportId), passportId) == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean addPassenger(String name, String passportId) {
        SecurityHashMap.SecurityHashMapEntry entry =
                hashMap.getEntry(calculateHashCode(passportId), passportId);
        if (entry != null) {
            if (entry.getPassportId().equals(passportId) && !entry.getValue().equals(name)) {
                System.err.print("Suspicious behaviour");
                return false;
            }

            if(entry.getPassportId().equals(passportId) && entry.getValue().equals(name)) {
                if (entry.getAttempts() < 5) {
                    hashMap.updateAttempts(calculateHashCode(passportId), passportId);
                    return true; // no point of adding it because it's already there
                } else {
                    System.err.print("Suspicious behaviour");
                    return false;
                }
            }
        }

        if (hashMap.put(calculateHashCode(passportId),passportId, name) != null) {
            return true;
        }

        return false;
    }

    @Override
    public int count() {
        return hashMap.size();
    }

    @Override
    public int getIndex(String passportId) {
        return hashMap.getIndex(calculateHashCode(passportId));
    }

    private int getNextPrimeFrom(int start) {

        // A prime number will be found, because of MATH
        while(!isPrime(start)) {
            start++;
        }

        return start;
    }

    private boolean isPrime(int number) {
        for (int i = 2; i <= number / 2; ++i) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }
}

/* Add any additional helper classes here */

/* A Non-generic HashMap for securityDB purposes */
class SecurityHashMap {
    private int size;
    private int capacity;
    private int maxCapacity;
    private SecurityHashMapEntry[] buckets;

    SecurityHashMap(int capacity, int maxCapacity) {
        size = 0;
        this.capacity = capacity;
        this.maxCapacity = maxCapacity;
        this.buckets = new SecurityHashMapEntry[capacity];
    }

    private void increaseToMaxCapacity() {
        SecurityHashMapEntry[] newBuckets = new SecurityHashMapEntry[maxCapacity];

        // Copy over from previous buckets
        // We assume that size < maxCapacity
        for (int i = 0; i < size; i++) {
            newBuckets[i] = buckets[i];
        }

        buckets = newBuckets;
        capacity = maxCapacity;
    }

    public SecurityHashMapEntry getEntry(int key, String original) {
        int hashIndex = getIndex(key);

        if (isEmpty()) {
            return null;
        }

        for (int i = 0; i < capacity; i++) {
            if (buckets[hashIndex] != null && buckets[hashIndex].getPassportId().equals(original)) {
                return buckets[hashIndex];
            }

            if (hashIndex + 1 < capacity) {
                hashIndex++; //
            } else {
                hashIndex = 0;
            }
        }
        return null;
    }

    public void updateAttempts(int key, String original) {
        int hashIndex = getIndex(key);

        if (isEmpty()) {
            return; // nothing to do
        }

        for (int i = 0; i < capacity; i++) {
            if (buckets[hashIndex] != null && buckets[hashIndex].getPassportId().equals(original)) {
                SecurityHashMapEntry entry = buckets[hashIndex];
                entry.setAttempts(entry.getAttempts() + 1);
            }

            if (hashIndex + 1 < capacity) {
                hashIndex++; //
            } else {
                hashIndex = 0;
            }
        }
    }

    public String put(int key, String passportId, String value) {
        int hashIndex = getIndex(key);
        SecurityHashMapEntry entry = new SecurityHashMapEntry(key, passportId, value);

        if (size == maxCapacity) {
            return null; // Maximum capacity reached
        }

        if (size == capacity) {
            increaseToMaxCapacity();
        }

        for (int i = 0; i < capacity; i++) {
            SecurityHashMapEntry entryFound = buckets[hashIndex];
            if (entryFound == null) {
                buckets[hashIndex] = entry;
                size++;
                return entry.getValue();
            }

            if (hashIndex + 1 < capacity) {
                hashIndex++; // probing
            } else {
                hashIndex = 0; // circulate
            }
        }

        return  entry.getValue();
    }

    /**
     *
     * Removes an entry.
     * Uses linear probing
     *
     * @param key the key to use to find the entry
     * @return null if key couldn't be found or is empty,
     * returns removed entry value otherwise
     */
    public String remove(int key, String original) {
        int hashIndex = getIndex(key);

        if (isEmpty()) {
            return null;
        }

        for (int i = 0; i < capacity; i++) {
            if (buckets[hashIndex] != null && buckets[hashIndex].getPassportId().equals(original)) {
                String value = buckets[hashIndex].getValue();
                buckets[hashIndex] = null;
                size--;
                return value;
            }

            if (hashIndex + 1 < capacity) {
                hashIndex++; // Keep probing
            } else {
                hashIndex = 0; // circulate
            }
        }

        // nothing was found, key does not exist
        return null;
    }

    /**
     *
     * Gets the entry pointed by the key. uses linear probing
     *
     * @param key the key of the entry to get
     * @return null is table is empty or null if jey is not found,
     * returns the entry at the key
     */
    public String get(int key) {
        int hashIndex = getIndex(key);

        if (isEmpty()) {
            return null;
        }

        for (int i = 0; i < capacity; i++) {
            if (buckets[hashIndex] != null && buckets[hashIndex].getKey() == key) {
                SecurityHashMapEntry entry = buckets[hashIndex];
                return entry.getValue();
            }

            if (hashIndex + 1 < capacity) {
                hashIndex++; //
            } else {
                hashIndex = 0;
            }
        }
        return null; // key ot found
    }

    public int size() {
        return size;
    }

    public int getIndex(int key) {
        return key % capacity;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    static class SecurityHashMapEntry {
        int key;
        int attempts;
        String passportId;
        String value;

        public SecurityHashMapEntry(int key, String passportId, String value) {
            attempts = 0;
            this.key = key;
            this.passportId = passportId;
            this.value = value;
        }

        public String getPassportId() {
            return passportId;
        }

        public void setPassportId(String passportId) {
            this.passportId = passportId;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public int getKey() {
            return key;
        }

        public void setKey(int key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}