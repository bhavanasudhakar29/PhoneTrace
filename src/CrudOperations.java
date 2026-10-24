import java.util.List;

/**
 * INTERFACE
 * A contract that every DAO (data access) class must follow.
 * Whatever the table is, the app can Create, Read, Update and Delete
 * its records using the same method names.
 *
 * T = the type of object the DAO works with (Phone, Owner, Repair...)
 */
public interface CrudOperations<T> {

    boolean add(T item);            // CREATE

    T getById(String id);           // READ one

    List<T> getAll();               // READ all

    boolean update(T item);         // UPDATE

    boolean delete(String id);      // DELETE
}
