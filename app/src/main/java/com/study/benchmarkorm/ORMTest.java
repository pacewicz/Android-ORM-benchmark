package com.study.benchmarkorm;

import android.content.Context;
import android.util.Pair;

import com.study.benchmarkorm.model.Book;
import com.study.benchmarkorm.model.Library;
import com.study.benchmarkorm.model.Person;

import java.util.ArrayList;
import java.util.List;

public abstract class ORMTest {

    protected final int NUMBER_OF_PASSES = 10;
    protected RandomObjectsGenerator randomObjectsGenerator = new RandomObjectsGenerator();

    protected static final int BOOKS_SIMPLE_BATCH_SIZE = 1000;
    protected static final int LIBRARIES_BALANCED_BATCH_SIZE = 50;
    protected static final int BOOKS_BALANCED_BATCH_SIZE = 50;
    protected static final int PERSONS_BALANCED_BATCH_SIZE = 50;
    protected static final int LIBRARIES_COMPLEX_BATCH_SIZE = 5;
    protected static final int BOOKS_COMPLEX_BATCH_SIZE = 500;
    protected static final int PERSONS_COMPLEX_BATCH_SIZE = 400;

    public ORMTest(Context context) {
        initDB(context);
    }

    public abstract void initDB(Context context);

    public abstract void writeSimple(List<Book> books);

    public abstract List<Book> readSimple(int booksQuantity);

    public abstract void updateSimple(List<Book> books);

    public abstract void deleteSimple(List<Book> books);

    public abstract void writeComplex(List<Library> libraries, List<Book> books, List<Person> persons);

    public abstract Pair<List<Library>, Pair<List<Book>, List<Person>>> readComplex(int librariesQuantity, int booksQuantity, int personsQuantity);

    public abstract void updateComplex(List<Library> libraries, List<Book> books, List<Person> persons);

    public abstract void deleteComplex(List<Library> libraries, List<Book> books, List<Person> persons);

    public boolean isEmpty() {
        return readSimple(1).isEmpty();
    }

    public void warmingUp() {
        final List<Book> books = new ArrayList<>();
        final List<Person> persons = new ArrayList<>();
        final List<Library> libraries = new ArrayList<>();
        List<Book> oneLibraryBooks;
        List<Person> oneLibraryPersons;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                final Library library = randomObjectsGenerator.nextLibrary();
                writeComplex(new ArrayList<Library>() {{
                    add(library);
                }}, new ArrayList<Book>(), new ArrayList<Person>());
                List<Library> bufLibraries = readComplex(Math.max(LIBRARIES_BALANCED_BATCH_SIZE, LIBRARIES_COMPLEX_BATCH_SIZE) + 2, 0, 0)
                        .first;
                libraries.add(bufLibraries.get(bufLibraries.size() - 1));

                oneLibraryBooks = randomObjectsGenerator.generateBooks(10, libraries.get(j));
                oneLibraryPersons = randomObjectsGenerator.generatePersons(10, libraries.get(j));
                books.addAll(oneLibraryBooks);
                persons.addAll(oneLibraryPersons);
            }
            writeComplex(new ArrayList<Library>(), books, persons);
            libraries.clear();
            books.clear();
            persons.clear();
        }
        Pair<List<Library>, Pair<List<Book>, List<Person>>> data =
                readComplex(10,
                        100, 100);
        deleteComplex(data.first,
                data.second.first,
                data.second.second);

        System.gc();
    }

    public float[] writeSimple(int writeNumber) {
        // main part
        float[] allTime = new float[NUMBER_OF_PASSES];
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        final Library library = randomObjectsGenerator.nextLibrary();
        writeComplex(new ArrayList<Library>() {{
            add(library);
        }}, new ArrayList<Book>(), new ArrayList<Person>());
        Library readLibrary = readComplex(writeNumber + 1, 0, 0).first.get(writeNumber);

        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            List<Book> books = randomObjectsGenerator.generateOrphanedBooks(BOOKS_SIMPLE_BATCH_SIZE);
            simpleProfiler.start();
            writeSimple(books);
            allTime[i] = simpleProfiler.stop();

            System.gc();
        }

        return allTime;
    }

    public float[] readSimple() throws ObjectsAreNotFullyLoadedException {
        float[] allTime = new float[NUMBER_OF_PASSES];
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            simpleProfiler.start();
            List<Book> books = readSimple(BOOKS_SIMPLE_BATCH_SIZE);
            if (!checkIfLoaded(new ArrayList<Library>(), books, new ArrayList<Person>())) {
                throw new ObjectsAreNotFullyLoadedException();
            }
            allTime[i] = simpleProfiler.stop();
            deleteSimple(books);

            System.gc();
        }

        return allTime;
    }

    public float[] updateSimple() {
        // main part
        float[] allTime = new float[NUMBER_OF_PASSES];
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            List<Book> books = readSimple(BOOKS_SIMPLE_BATCH_SIZE);
            for (Book book : books) {
                book.setAuthor(randomObjectsGenerator.nextString());
            }
            simpleProfiler.start();
            updateSimple(books);
            allTime[i] = simpleProfiler.stop();

            System.gc();
        }

        return allTime;
    }

    public float[] deleteSimple() {
        float[] allTime = new float[NUMBER_OF_PASSES];
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            List<Book> books = readSimple(BOOKS_SIMPLE_BATCH_SIZE);
            simpleProfiler.start();
            deleteSimple(books);
            allTime[i] = simpleProfiler.stop();

            System.gc();
        }

        return allTime;
    }

    public float[] writeBalanced(int writeNumber) {
        return writeComplexBenchmark(BOOKS_BALANCED_BATCH_SIZE, LIBRARIES_BALANCED_BATCH_SIZE, PERSONS_BALANCED_BATCH_SIZE, writeNumber);
    }

    protected float[] writeComplexBenchmark(int booksBatchSize, int librariesBatchSize, int personsBatchSize, int writeNumber) {

        final List<Book> books = new ArrayList<>(booksBatchSize * librariesBatchSize);
        final List<Person> persons = new ArrayList<>(personsBatchSize * librariesBatchSize);
        List<Library> libraries;
        List<Book> oneLibraryBooks;
        List<Person> oneLibraryPersons;

        // main part
        float[] allTime = new float[NUMBER_OF_PASSES];
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            allTime[i] = 0;

            //libraries
            for (int j = 0; j < librariesBatchSize; j++) {
                final Library library = randomObjectsGenerator.nextLibrary();

                simpleProfiler.start();
                writeComplex(new ArrayList<Library>() {{
                    add(library);
                }}, new ArrayList<Book>(), new ArrayList<Person>());
                allTime[i] += simpleProfiler.stop();
            }
        }

        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            libraries = readComplex((i + 1) * librariesBatchSize * (writeNumber + 1), 0, 0).first
                    .subList(i * librariesBatchSize * writeNumber,
                            (i + 1) * librariesBatchSize * (writeNumber + 1));

            //books and persons
            for (int j = 0; j < librariesBatchSize; j++) {
                oneLibraryBooks = randomObjectsGenerator.generateBooks(booksBatchSize, libraries.get(j));
                oneLibraryPersons = randomObjectsGenerator.generatePersons(personsBatchSize, libraries.get(j));
                books.addAll(oneLibraryBooks);
                persons.addAll(oneLibraryPersons);
            }

            simpleProfiler.start();
            writeComplex(new ArrayList<Library>(), books, persons);
            allTime[i] += simpleProfiler.stop();

            books.clear();
            persons.clear();

            System.gc();
        }

        return allTime;
    }

    public float[] readBalanced() throws ObjectsAreNotFullyLoadedException {
        return readComplexBenchmark(BOOKS_BALANCED_BATCH_SIZE, LIBRARIES_BALANCED_BATCH_SIZE, PERSONS_BALANCED_BATCH_SIZE);
    }

    protected float[] readComplexBenchmark(int booksBatchSize, int librariesBatchSize, int personsBatchSize) throws ObjectsAreNotFullyLoadedException {
        booksBatchSize *= librariesBatchSize;
        personsBatchSize *= librariesBatchSize;

        float[] allTime = new float[NUMBER_OF_PASSES];
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            simpleProfiler.start();
            Pair<List<Library>, Pair<List<Book>, List<Person>>> data = readComplex(librariesBatchSize, booksBatchSize, personsBatchSize);
            if (!checkIfLoaded(data.first, data.second.first, data.second.second)) {
                throw new ObjectsAreNotFullyLoadedException();
            }
            allTime[i] = simpleProfiler.stop();
            deleteComplex(new ArrayList<Library>(), data.second.first, data.second.second);

            System.gc();
        }
        return allTime;
    }

    public float[] updateBalanced() {
        return updateComplexBenchmark(BOOKS_BALANCED_BATCH_SIZE, LIBRARIES_BALANCED_BATCH_SIZE, PERSONS_BALANCED_BATCH_SIZE);
    }

    protected float[] updateComplexBenchmark(int booksBatchSize, int librariesBatchSize, int personsBatchSize) {
        booksBatchSize *= librariesBatchSize;
        personsBatchSize *= librariesBatchSize;

        // main part
        float[] allTime = new float[NUMBER_OF_PASSES];
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            Pair<List<Library>, Pair<List<Book>, List<Person>>> readed = readComplex(librariesBatchSize, booksBatchSize, personsBatchSize);
            List<Library> libraries = readed.first;
            List<Book> books = readed.second.first;
            List<Person> persons = readed.second.second;

            for (Library library : libraries) {
                library.setName(randomObjectsGenerator.nextString());
            }

            for (Book book : books) {
                book.setAuthor(randomObjectsGenerator.nextString());
            }

            for (Person person : persons) {
                person.setFirstName(randomObjectsGenerator.nextString());
                person.setSecondName(randomObjectsGenerator.nextString());
            }

            simpleProfiler.start();
            updateComplex(libraries, books, persons);
            allTime[i] = simpleProfiler.stop();

            System.gc();
        }

        return allTime;
    }

    public float[] deleteBalanced() {
        return deleteComplexBenchmark(BOOKS_BALANCED_BATCH_SIZE, LIBRARIES_BALANCED_BATCH_SIZE, PERSONS_BALANCED_BATCH_SIZE);
    }

    protected float[] deleteComplexBenchmark(int booksBatchSize, int librariesBatchSize, int personsBatchSize) {
        booksBatchSize *= librariesBatchSize;
        personsBatchSize *= librariesBatchSize;

        float[] allTime = new float[NUMBER_OF_PASSES];
        SimpleProfiler simpleProfiler = new SimpleProfiler();
        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            Pair<List<Library>, Pair<List<Book>, List<Person>>> data = readComplex(0, booksBatchSize, personsBatchSize);
            simpleProfiler.start();
            deleteComplex(new ArrayList<Library>(), data.second.first, data.second.second);
            allTime[i] = simpleProfiler.stop();

            System.gc();
        }

        for (int i = 0; i < NUMBER_OF_PASSES; i++) {
            Pair<List<Library>, Pair<List<Book>, List<Person>>> data = readComplex(librariesBatchSize, 0, 0);
            simpleProfiler.start();
            deleteComplex(data.first, new ArrayList<Book>(), new ArrayList<Person>());
            allTime[i] += simpleProfiler.stop();

            System.gc();
        }
        
        Pair<List<Library>, Pair<List<Book>, List<Person>>> data = readComplex(librariesBatchSize * NUMBER_OF_PASSES, 0, 0);
        deleteComplex(data.first, new ArrayList<Book>(), new ArrayList<Person>());

        return allTime;
    }

    public float[] writeComplex(int writeNumber) {
        return writeComplexBenchmark(BOOKS_COMPLEX_BATCH_SIZE,
                LIBRARIES_COMPLEX_BATCH_SIZE, PERSONS_COMPLEX_BATCH_SIZE, writeNumber);
    }

    public float[] readComplex() throws ObjectsAreNotFullyLoadedException {
        return readComplexBenchmark(BOOKS_COMPLEX_BATCH_SIZE, LIBRARIES_COMPLEX_BATCH_SIZE, PERSONS_COMPLEX_BATCH_SIZE);
    }

    public float[] updateComplex() {
        return updateComplexBenchmark(BOOKS_COMPLEX_BATCH_SIZE, LIBRARIES_COMPLEX_BATCH_SIZE, PERSONS_COMPLEX_BATCH_SIZE);
    }

    public float[] deleteComplex() {
        return deleteComplexBenchmark(BOOKS_COMPLEX_BATCH_SIZE, LIBRARIES_COMPLEX_BATCH_SIZE, PERSONS_COMPLEX_BATCH_SIZE);
    }
    
    
    public static class ObjectsAreNotFullyLoadedException extends Exception {
        public ObjectsAreNotFullyLoadedException() {
            super("some objects were not fully loaded");
        }

        public ObjectsAreNotFullyLoadedException(String message) {
            super(message);
        }
    }
    
    public boolean checkIfLoaded(List<Library> libraries, List<Book> books, List<Person> persons) {
        for (Library library: libraries) {
            if (library.getName() == null) {
                return false;
            }
            if (library.getAddress() == null) {
                return false;
            }
        }
        for (Person person: persons) {
            if (person.getFirstName() == null) {
                return false;
            }
            if (person.getSecondName() == null) {
                return false;
            }
            if (person.getBirthdayDate() == null) {
                return false;
            }
            if (person.getGender() == null) {
                return false;
            }
            Library library = person.getLibrary();
            if (library == null) {
                return false;
            }
            if (library.getName() == null) {
                return false;
            }
            if (library.getAddress() == null) {
                return false;
            }
        }
        for (Book book: books) {
            if (book.getAuthor() == null) {
                return false;
            }
            if (book.getTitle() == null) {
                return false;
            }
            Library library = book.getLibrary();
            if (library == null) {
                return false;
            }
            if (library.getName() == null) {
                return false;
            }
            if (library.getAddress() == null) {
                return false;
            }
        }
        return true;
    }
}
