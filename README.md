# Assignment 4 — Facade Pattern

## Topic

Library Book Borrowing System. The client uses `LibraryFacade` to borrow and return books. The four subsystem classes have separate responsibilities:

| Class | Responsibility |
| --- | --- |
| `MemberRegistry` | Checks whether a member exists and is active. |
| `BookCatalog` | Tracks copies and reserves or releases a book. |
| `LoanRegistry` | Tracks 14-day loans, a three-loan limit, and duplicate borrowing. |
| `FineService` | Calculates overdue fines, tracks debt, and accepts payments. |

`Main` creates and calls only `LibraryFacade`. Subsystems do not refer to the facade and can also be used directly. The facade exposes only three simple operations: `borrowBook`, `returnBook`, and `payFine`.

## Run

Requires JDK 17 or newer. Run from the repository root:

```sh
javac -encoding UTF-8 -Xlint:all -Werror -d out src/library/*.java
java -cp out library.Main
```

The example borrows the only copy of `B001`, rejects another request while it is unavailable, returns the book, then borrows the released copy for another member. Sample members are `S001` and `S003` (active) and `S002` (inactive). `B001` has one copy; `B002` has two. Data exists only in memory.

## How the facade works

`borrowBook` checks the member, unpaid fines, and loan limit. It reserves a book and creates the loan. If loan creation fails, the facade releases the reservation. It returns a loan ID that the client can use to return the book.

`returnBook` finds the loan, calculates `100.00 KZT` per overdue day, records the return, charges the fine once, and releases the book. Returning the same loan again leaves the stock and debt unchanged. `payFine` lets the member clear debt before borrowing another book.

## Clean Code principles

1. **Meaningful names:** `borrowBook`, `reserve`, and `requireActive` describe their actions.
2. **Single responsibility:** Each subsystem owns one part of the library process; the facade only coordinates them.
3. **Small methods:** Checking a member, reserving a copy, and calculating a fine are separate operations.
4. **Encapsulation:** State is private; clients receive a loan ID or fine amount instead of subsystem objects.
5. **No duplicate business rules:** The 14-day term and three-loan limit live in `LoanRegistry`; fine calculation lives in `FineService`.
6. **Error handling:** Invalid members, unavailable books, and unpaid fines stop borrowing. A failed loan operation releases its book reservation.

Facade is a structural pattern: it gives the client a simple interface to four collaborating subsystem classes. The subsystems remain independent of the facade.
