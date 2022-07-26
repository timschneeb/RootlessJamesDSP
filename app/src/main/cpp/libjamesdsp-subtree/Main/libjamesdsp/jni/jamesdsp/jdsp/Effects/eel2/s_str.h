/* Purpose:         This lightweight library for "smart" strings is meant to mimick how strings function in C++.
 * Authors Note:    The user should interface with the type "s_str", which is guaranteed to always be memory safe and contain a terminating null character.
 *                  A "s_str" should ONLY be instantiated/assigned with a call to one of the varying s_str_create functions and MUST be later 
 *                  deconstructed/deallocated via a call to s_str_destroy.
 */
#ifndef S_STRING_H
#define S_STRING_H
#include <string.h>
/* The first parameter of every function is a pointer-to-s_str since all functions use/manipulate the
 * s_str's value (and because of the fact that there is no "this" keyword in C). 
 *
 * Note that calling functions which cause the s_str to dynamically reallocate due to the addition/subtraction of 
 * member(s) may not do anything at all if the needed space in memory cannot be allocated, leaving the s_str in a 
 * guaranteed unchanged/safe state. Memory safety is #1 priority.
 *
 * Many functions pertaining to the s_str type allow you to add/remove '\0'. These are treated like every-other
 * character and a s_str is not limited to a single '\0'. A call to the s_str_c_str or s_str_c_substr function can 
 * return a const pointer-to-char that points to the held string containing multiple terminating null characters. The point is, 
 * it is often a sign of bad programming practice if you are manually inserting/deleting terminating null characters and doing so 
 * can easily lead to unexpected results in user-defined code (although it is completely legal). This should go without saying, 
 * but to be clear, the terminating null character is removed and appended to the end of the s_str whenever you add a
 * char/C-string/s_str to a s_str. No matter what, it is guaranteed that the s_str will have a terminating null character.
 */
/* Typedef that should be used exclusively!
 *
 * Example:
 *      s_str foo = s_str_create_from_c_str("pogchamp");
 *      int str_length = s_str_length(&foo);
 *      s_str_clear(&foo);
 *      s_str_append_c_str(&foo,"bar1235654241dsfa");
 *      printf("%s\n", s_str_c_str(&foo));
 *      s_str_destroy(&foo);
 */
typedef void *s_str;
/* Dynamically allocates memory to create a new s_str that contains only a default terminating null character. The s_str MUST call s_str_destroy 
 * to free memory at some point.
 *
 * Returns a unique and newly made s_str, or NULL if memory cannot be allocated to hold the s_str.
 */
s_str s_str_create(void);
/* Dynamically allocates memory to create a new s_str that contains the char and a default terminating null character. The s_str MUST call 
 * s_str_destroy to free memory at some point.
 *
 * Returns a unique and newly made s_str, or NULL if memory cannot be allocated to hold the s_str.
 */
s_str s_str_create_from_char(const char c);
/* Dynamically allocates memory to create a new s_str that contains the char for the given length and a default terminating null character. The s_str MUST 
 * call s_str_destroy to free memory at some point.
 *
 * Returns a unique and newly made s_str, or NULL if memory cannot be allocated to hold the s_str.
 */
s_str s_str_create_from_multi_char(const size_t length, const char c);
/* Dynamically allocates memory to create a new s_str. The s_str MUST call s_str_destroy to free memory at some point.
 *
 * Takes in either a C-string literal (or any memory address to char that has a terminating null character at somepoint in memory) or NULL. 
 * If c_str_ptr == NULL, then the s_str is default initialized (i.e. the "string" it represents will only be made up of a terminating null character); 
 * otherwise, the s_str is set equal to a copy of the C-string.
 *
 * Returns a unique and newly made s_str, or NULL if memory cannot be allocated to hold the s_str.
 */
s_str s_str_create_from_c_str(const char *const c_str_ptr);
s_str s_str_create_from_c_str_0Inc(const char *const c_str_ptr, const size_t length);
/* Dynamically allocates memory to create a new s_str. The s_str MUST call s_str_destroy to free memory at some point.
 *
 * Takes in either the memory address of a s_str or NULL. If s_str_ptr_for_create == NULL, then the s_str is default initialized (i.e. the "string" it 
 * represents will only be made up of a terminating null character); otherwise, the s_str is set equal to a copy of the (dereferenced) s_str_ptr_for_create.
 *
 * Returns a unique and newly made s_str, or NULL if memory cannot be allocated to hold the s_str.
 */
s_str s_str_create_from_s_str(const s_str *const s_str_ptr_for_create);
/* Attempts to free allocated memory relating to the s_str. The s_str is then set equal to NULL. 
 *
 * If the s_str is invalid or already freed, this function simply does nothing.
 */
void s_str_destroy(s_str *const this_);
/* Retrieves and returns a C-style string guaranteed to have a terminating null character.
 *
 * This function is incredibly important in library/user defined string manipulation because it doesn't rely on the
 * implementation of a s_str. 
 *
 * Note that the contents of the returned C-string cannot be altered for the sake of memory safety (hence the constness)!
 *
 * Its worth mentioning that implementation does allow the user to freely add/remove '\0' via manipulation functions
 * just like any other char (except there will ALWAYS be a default terminating null character located at the end of the s_str). This behavior is 
 * defined; however, the user should be wary and avoid doing this because upon calling s_str_c_str, the returned C-string will contain the 
 * original terminating null character at the end, plus any additional terminating null character's throughout. Unexpected outcomes to user defined 
 * functionality may occur!
 */
const char* s_str_c_str(const s_str *const this_);
/* Retrieves and returns a C-style string starting at "index" that is guaranteed to have a terminating null character.
 *
 * If "index" is out of range, a pointer-to-const-char is returned that points to the default terminating null
 * character.
 *
 * This function behaves similarly to s_str_c_str.
 */
const char* s_str_c_substr(const s_str *const this_, const size_t index);
/* Sets the s_str equal to the char, clearing all previously stored elements.
 */
void s_str_assign_char(s_str *const this_, const char c);
/* Sets the s_str equal to the char for the given length, clearing all previously stored elements.
 */
void s_str_assign_multi_char(s_str *const this_, const size_t length, const char c);
/* Sets the s_str equal to a copy of the C-string, clearing all previously stored elements.
 */
void s_str_assign_c_str(s_str *const this_, const char *const c_str_ptr);
/* Sets the s_str equal to a copy of the (dereferenced) s_str_ptr_for_assign, clearing all previously stored elements.
 */
void s_str_assign_s_str(s_str *const this_, const s_str *const s_str_ptr_for_assign);
/* Retrieves and returns the char at "index" in the s_str. Returns '\0' if "index" is out of range.
 */
char s_str_at(const s_str *const this_, const size_t index);
/* Removes the char located at "index". If "index" is out of range or is the index of the default terminating null
 * character, this function does nothing.
 */
void s_str_remove_at(const s_str *const this_, const size_t index);
/* Attempts to find the first appearance of the char, starting from the front and ending at the back of the s_str.
 * Returns the index of the first matching char if found; otherwise, returns the index of the default terminating null character.
 */
size_t s_str_find_char(const s_str *const this_, const char c);
/* Attempts to find the first appearance of the c_str, starting from the front and ending at the back of the s_str.
 * Returns the index at the beginning of the first matching c_str if found. If not found or if the C-string is larger in size than the s_str, 
 * returns the index of the default terminating null character.
 */
size_t s_str_find_c_str(const s_str *const this_, const char *const c_str_ptr);
/* Attempts to find the first appearance of the (dereferenced) s_str_ptr_for_find, starting from the front and ending at the back of the s_str. 
 * Returns the index at the beginning of the first matching string if found. If not found or if the (dereferenced) s_str_ptr_for_find is larger 
 * in size than the s_str, returns the index of the default terminating null character.
 */
size_t s_str_find_s_str(const s_str *const this_, const s_str *const s_str_ptr_for_find);
/* Attempts to find the first appearance of the char, starting from the back and ending at the front of the s_str.
 * Returns the index of the first matching char if found; otherwise, returns the index of the default terminating null character.
 */
size_t s_str_rfind_char(const s_str *const this_, const char c);
/* Attempts to find the first appearance of the c_str, starting from the back and ending at the front of the s_str.
 * Returns the index at the beginning of the first matching c_str if found. If not found or if the C-string is larger in size than the s_str, 
 * returns the index of the default terminating null character.
 */
size_t s_str_rfind_c_str(const s_str *const this_, const char *const c_str_ptr);
/* Attempts to find the first appearance of the (dereferenced) s_str_ptr_for_rfind, starting from the back and ending at the front of the s_str.
 * Returns the index at the beginning of the first matching string if found. If not found or if the (dereferenced) s_str_ptr_for_rfind is larger 
 * in size than the s_str, returns the index of the default terminating null character.
 */
size_t s_str_rfind_s_str(const s_str *const this_, const s_str *const s_str_ptr_for_rfind);
/* Returns the maximum number of elements the s_str can store (including the terminating null character) before
 * needing to dynamically reallocate. 
 *
 * Note: Dynamic reallocation is all automatic and the user should not worry about it unless reallocation time is
 * expensive (which is unlikely in even huge strings since char is only 1 byte).
 *
 * Note: The capacity of a s_str is always greater than or equal to the s_str's size. The capacity represents the
 * max number of elements the s_str can store before needing to reallocate.
 */
size_t s_str_capacity(const s_str *const this_);
/* Requests the s_str to reserve enough space in memory so that the s_str can store the given "num_elem's" before needing to 
 * reallocate. If "num_elem's" is less than or equal to the current s_str capacity, the request is denied.
 */
void s_str_reserve(s_str *const this_, const size_t num_elem);
/* Returns the size of the s_str (the total number of held elements). 
 *
 * The returned total will count every single character present, including terminating null characters.
 */
size_t s_str_size(const s_str *const this_);
/* Returns the "strlen" of the s_str. 
 *
 * It is important to realize that if the s_str contains multiple terminating null characters, the returned length will only 
 * be the size of the s_str up to but not including the first terminating null character.
 */
size_t s_str_length(const s_str *const this_);
/* Returns true if the s_str is empty (excluding the default terminating null character).
 * Returns false if the s_str contains an element other than the default terminating null character.
 */
int s_str_empty(const s_str *const this_);
/* Clears all elements in the s_str.
 */
void s_str_clear(const s_str *const this_);
/* Removes any preceding or trailing spaces in the s_str.
 *  Example:
 *      s_str foo = s_str_create_from_c_str("  test ");
 *      s_str_trim(&foo);
 *      printf("\"%s\"\n", s_str_c_str(&foo));
 *      // Output: "test"
 *      s_str_destroy(&foo);
 */
void s_str_trim(const s_str *const this_);
/* Requests the s_str to reduce its capacity to fit its size. 
 */
void s_str_shrink_to_fit(s_str *const this_);
/* Adds the char to the end of the s_str.
 */
void s_str_push_back(s_str *const this_, const char c);
/* Removes the char (excluding the default terminating null character) located at the end of the s_str.
 * 
 * Returns the removed char.
 */
char s_str_pop_back(const s_str *const this_);
/* Appends the c_str to the end of the s_str.
 */
void s_str_append_c_str(s_str *const this_, const char *const c_str_ptr);
/* Appends the (dereferenced) s_str_ptr_for_append to the end of the s_str.
 */
void s_str_append_s_str(s_str *const this_, const s_str *const s_str_ptr_for_append);
/* Retrieves and returns the memory address of the s_str's first word. This function does the same thing as s_str_c_str
 * but is more explicit in its intention when paired with the functions s_str_remove_first_word, s_str_last_word, and
 * s_str_remove_last_word.
 */
const char* s_str_first_word(const s_str *const this_);
/* Removes s_str's first word plus any trailing spaces. The first word is either the string of characters preceding the first space, the first
 * space itself, or the entire s_str if there are no spaces.
 */
void s_str_remove_first_word(const s_str *const this_);
/* Retrieves and returns the memory address of the s_str's last word. The beginning of the last word is the string of
 * characters following the last space, the last space itself, or if there are no spaces, the entire s_str.
 */
const char* s_str_last_word(const s_str *const this_);
/* Removes the s_str's last word plus any preceding spaces. The last word is either the string of characters following
 * the last space, the last space itself, or if there are no spaces, the entire s_str.
 */
void s_str_remove_last_word(const s_str *const this_);
/* Erases the portion of the s_str that begins at the character position "index" and spans "length" characters (or until the end of the s_str).
 *
 * Range: [index, length)
 */
void s_str_erase(const s_str *const this_, const size_t index, size_t length);
/* Inserts the char into the s_str right before the character at "index".
 */
void s_str_insert_char(s_str *const this_, const size_t index, const char c);
/* Inserts the C-string into the s_str right before the character at "index".
 */
void s_str_insert_c_str(s_str *const this_, const size_t index, const char *const c_str_ptr);
/* Inserts the (dereferenced) s_str_ptr_for_insert into the s_str right before the s_str's character at "index".
 */
void s_str_insert_s_str(s_str *const this_, const size_t index, const s_str *const s_str_ptr_for_insert);
/* Seperates every element in the s_str with char. If the size of the s_str is 1, then this function does nothing.
 */
void s_str_sepwith_char(s_str *const this_, const char c);
/* Seperates every element in the s_str with the C-string. If the size of the s_str is 1, then this function does nothing.
 */
void s_str_sepwith_c_str(s_str *const this_, const char *const c_str_ptr);
/* Seperates every element in the s_str with the (dereferenced) s_str_ptr_for_sepwith. If the size of the s_str is 1, then
 * this function does nothing.
 */
void s_str_sepwith_s_str(s_str *const this_, const s_str *const s_str_ptr_for_sepwith);
/* Replaces the portion of the s_str that begins at "index" and spans "length" characters with char (or until the end of
 * the s_str).
 *
 * Range: [index, length)
 */
void s_str_replace(const s_str *const this_, const size_t index, size_t length, const char c);
/* Replaces the entire s_str with char.
 */
void s_str_replace_all(const s_str *const this_, const char c);
/* Swaps the elements in the s_str's.
 *
 * Example:
 *      s_str str1 = s_str_create_from_c_str("1234");
 *      s_str str2 = s_str_create_from_c_str("abcdef");
 *      s_str_swap(&str1, &str2);
 *      printf("str1: %s, str2: %s\n", s_str_c_str(&str1), s_str_c_str(&str2));
 *      // Output: str1: abcdef, str2: 1234
 *      s_str_destroy(&str1);
 *      s_str_destroy(&str2);
 */
void s_str_swap(s_str *const this_, s_str *const s_str_ptr_for_swap);
/* Calls the given function (which takes a pointer-to-char and returns nothing (void)) for each element in the s_str that begins at the 
 * character position "index" and spans "length" characters (or until the end of the s_str). The default terminating null character is 
 * excluded.
 *
 * Range: [index, length)
 */
void s_str_foreach(const s_str *const this_, const size_t index, size_t length, void (*const fptr)(char *));
/* Calls the given function (which takes a pointer-to-char and returns nothing (void)) for each element in the s_str. The default 
 * terminating null character is excluded.
 */
void s_str_foreach_all(const s_str *const this_, void (*const fptr)(char *));
/* Compares the s_str with the C-string (excludes the default terminating null character and the C-string's terminating
 * null character).
 *
 * Returns 0 if they compare equal.
 *
 * Returns < 0 if either the value of the first character that does not match is lower in the s_str, or all
 * compared characters match but the s_str is shorter in size.
 *
 * Returns > 0 if either the value of the first character that does not match is greater in the s_str, or all
 * compared characters match but the s_str is longer in size.
 */
int s_str_compare_c_str(const s_str *const this_, const char *const c_str_ptr);
/* Compares the s_str with the (dereferenced) s_str_ptr_for_compare (excludes the default terminating null character in
 * both s_str's).
 *
 * Returns 0 if they compare equal.
 *
 * Returns < 0 if either the value of the first character that does not match is lower in the s_str, or all
 * compared characters match but the s_str is shorter in size.
 *
 * Returns > 0 if either the value of the first character that does not match is greater in the s_str, or all
 * compared characters match but the s_str is longer in size.
 */
int s_str_compare_s_str(const s_str *const this_, const s_str *const s_str_ptr_for_compare);
/* Sorts the s_str's elements by using the function pointer "compare" in a fashion identical to qsort (omits the
 * default terminating null character from sort).
 */
void s_str_sort(s_str *const this_, int (*const compare)(const void *, const void *));
#endif