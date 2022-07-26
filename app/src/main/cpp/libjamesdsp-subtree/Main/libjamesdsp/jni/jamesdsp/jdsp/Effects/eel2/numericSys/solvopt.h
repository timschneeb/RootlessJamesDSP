#ifndef SOLVOPT_H
#define SOLVOPT_H
#include <stdint.h>
typedef struct
{
	double const *H, *f, *A, *b;
	int32_t dimX, dimY;
	double *tmp1, *tmp2;
} SharedFunctionVariables;
/*
   solvopt_options[0]= H, where sign(H)=-1 resp. sign(H)=+1 means minimize
               resp. maximize FUN (valid only for unconstrained problem)
               and H itself is a factor for the initial trial step size 
               (options[0]=-1.e0 by default),
   solvopt_options[1]= relative error for the argument
               in terms of the max-norm (1.e-4 by default),
   solvopt_options[2]= relative error for the function value (1.e-6 by default),
   solvopt_options[3]= limit for the number of iterations (15000 by default),
   solvopt_options[4]= control of the display of intermediate results and
               error resp. warning messages (default value is 0,
               i.e., no intermediate output but error and warning
               messages),
   solvopt_options[5]= admissible maximal residual for a set of constraints
               (options[5]=1.e-8 by default),
  @solvopt_options[6]= the coefficient of space dilation (2.5 by default),
  @solvopt_options[7]= the lower bound for the stepsize used for the finite
               difference approximation of gradients (1.e-11 by default).
  (@ ... changes should be done with care)
Returned optional values:
        options[8], the number of iterations, options[8]<0 means 
                    an error occured,
        options[9], the number of objective function evaluations,
        options[10],the number of gradients evaluations,
*/

double solvopt(uint32_t n, double x[], double fun(double *, SharedFunctionVariables *), void grad(double *, double *, SharedFunctionVariables *), double options[], double func(double *, SharedFunctionVariables *), void gradc(double *, double *, SharedFunctionVariables *), SharedFunctionVariables *pass);
#endif
