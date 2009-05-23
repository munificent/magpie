using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// Contains the context in which expression or declaration binding occurs.
    /// For example, the current name context for looking up names, the set
    /// of type arguments, etc.
    /// </summary>
    public class BindingContext
    {
        public Compiler Compiler { get; private set; }
        public NameContext NameContext { get; private set; }
        public IDictionary<string, IBoundDecl> TypeArguments { get; private set; }

        public BindingContext(Compiler compiler, NameContext context)
            : this(compiler, context, new Dictionary<string, IBoundDecl>())
        {
        }

        public BindingContext(Compiler compiler, NameContext context, IDictionary<string, IBoundDecl> typeArguments)
        {
            Compiler = compiler;
            NameContext = context;
            TypeArguments = typeArguments;
        }
    }
}
