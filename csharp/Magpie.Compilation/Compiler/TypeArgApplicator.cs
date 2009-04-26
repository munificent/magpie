using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public class TypeArgApplicator
    {
        public static TypeArgApplicator Create(Function generic, IList<Decl> typeArgs, IEnumerable<Decl> argTypes)
        {
            TypeArgApplicator applicator = new TypeArgApplicator();

            // try to infer the args if not passed in
            IList<Decl> inferredTypeArgs = TypeArgInferrer.Infer(generic, argTypes);
            applicator.mCanInfer = inferredTypeArgs != null;

            if ((typeArgs == null) || (typeArgs.Count == 0))
            {
                typeArgs = inferredTypeArgs;
            }

            // number of type arguments must match
            if ((typeArgs == null) || (typeArgs.Count != generic.TypeParameters.Count)) return null;

            // bind the type arguments
            for (int i = 0; i < typeArgs.Count; i++)
            {
                string name = ((NamedType)generic.TypeParameters[i]).Name;
                applicator.mInstancedTypes[name] = typeArgs[i];
            }

            return applicator;
        }

        public bool CanInfer { get { return mCanInfer; } }

        public Decl ApplyType(Decl decl)
        {
            NamedType named = decl as NamedType;
            if (named != null) return ApplyType(named);

            return decl;
        }

        public Decl ApplyType(NamedType type)
        {
            // look up the generic type
            Decl appliedType;
            if (!mInstancedTypes.TryGetValue(type.Name, out appliedType))
            {
                appliedType = type;
            }

            // if the type itself has type arguments, apply them too
            if (type.IsGeneric)
            {
                // translate the args too
                NamedType namedGeneric = appliedType as NamedType;

                if (namedGeneric == null) throw new CompileException("Cannot instantiate a generic type with type arguments using a non-named type. In other words, what the hell is int[A, B] supposed to mean?");

                return new NamedType(namedGeneric.Name, namedGeneric.TypeArgs.Select(decl => ApplyType(decl)));
            }

            return appliedType;
        }

        public IEnumerable<Decl> ApplyTypes(IEnumerable<Decl> types)
        {
            foreach (Decl decl in types)
            {
                NamedType named = decl as NamedType;
                if (named != null)
                {
                    yield return ApplyType(named);
                }
                else
                {
                    yield return decl;
                }
            }
        }

        private TypeArgApplicator()
        {
        }

        private Dictionary<string, Decl> mInstancedTypes = new Dictionary<string, Decl>();
        private bool mCanInfer;
    }
}
