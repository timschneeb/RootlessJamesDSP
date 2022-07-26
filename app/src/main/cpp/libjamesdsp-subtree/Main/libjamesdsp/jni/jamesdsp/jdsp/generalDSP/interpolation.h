typedef struct
{
	double* x_;
	double* y_;
	double* dydx_;
	int n;
} cubic_hermite;
typedef struct
{
	cubic_hermite cb;
	double *s;
} ierper;
extern void initIerper(ierper *intp, int n);
extern void freeIerper(ierper *intp);
extern double getValueAt(cubic_hermite *cuher, double x);
extern double getDerivativeAt(cubic_hermite *cuher, double x);
extern void pchip(ierper *intp, double * x, double * y, int n, int left_endpoint_derivative, int right_endpoint_derivative);
extern void makima(ierper *intp, double * x, double * y, int n, int left_endpoint_derivative, int right_endpoint_derivative);