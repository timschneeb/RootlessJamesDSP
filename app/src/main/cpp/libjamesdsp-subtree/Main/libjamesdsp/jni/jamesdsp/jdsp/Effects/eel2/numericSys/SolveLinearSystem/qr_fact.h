#ifndef QR_FACT_H
#define QR_FACT_H
typedef struct
{
	double *tmp1, *tmp2, *tau_data, *vn1_data, *vn2_data, *work_data;
} QR_heap;
void preallocateMemQR(QR_heap *qr, int m, int n);
void freeMemQR(QR_heap *qr);
void QR_pivoting(QR_heap *qr, double *Amat, int m, int n, double *Qmat, double *Rmat, int *perm);
#endif