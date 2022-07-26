#ifndef QUADPROG_H
#define QUADPROG_H
#include <stdint.h>
extern int32_t cpoly(double *opr, double *opi, int32_t degree, double *zeror, double *zeroi);
extern void init_genrand(unsigned long s);
extern unsigned long genrand_int32(void);
extern double genrand_real2(void);
extern double genrand_res53(void);
extern void transpose(double *src, double *dst, int32_t n, int32_t m);
extern int32_t rank(const double *mat, const int32_t n, const int32_t m);
extern void matrix_matrix_product(const double *a, const double *b, double *product, int32_t rows1, int32_t cols1, int32_t rows2, int32_t cols2, double weight);
extern void transpose_matrix_matrix_self_product(const double *a, double *product, int32_t rows1, int32_t cols1);
extern void matrix_transpose_matrix_self_product(const double *a, double *product, int32_t rows1, int32_t cols1);
extern double unroll_dot_product(const double *x, const double *y, int32_t n, double weight);
extern void matrix_vector_mult(const double *mat, const double *vec, double *c, int32_t rows, int32_t cols, double weight);
extern void transpose_matrix_vector_mult(const double *mat, const double *vec, double *c, int32_t rows, int32_t cols, double weight);
extern double determinant(const double *a, int32_t n);
extern void mldivide(const double A[], const int32_t rows1, const int32_t cols1, const double b[], const int32_t rows2, const int32_t cols2, double Y[], int32_t Y_size[2]);
void NxN_mldivide_mat_vec(const double *A, const double f[], const int32_t n, double H[]);
extern void mrdivide(const double A[], const int32_t rows1, const int32_t cols1, const double b[], const int32_t rows2, const int32_t cols2, double Y[], int32_t Y_size[2]);
extern void inv(const double *A, const int32_t n, double *Y);
extern void pinv(double *mat, int32_t m, int32_t n, double *Y, int32_t *size);
unsigned int cholesky(double *A, double *L, unsigned int n);
void inv_chol(double *L, double *Y, unsigned int n);
void geninv(double *G, unsigned int m1, unsigned int n1, double *Y, unsigned int size[2]);
int32_t quadprog(int problemLen, double *H, double *f, int inequalityLen, float *A, float *b, int equalityLen, float *Aeq, float *beq, float *lb, float *ub, float *outAns, float *fv);
#endif