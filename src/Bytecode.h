#pragma once

#define OP_MOVE       (0x01) // A: from, B: to
#define OP_LITERAL    (0x03) // AB: index, C: register
#define OP_CALL       (0x04) // A: arg, B: method, C: result
#define OP_RETURN     (0x05) // A: result
#define OP_HACK_PRINT (0x06) // A: register

#define MAKE_MOVE(from, to)       ((from << 24) | (to << 16) | OP_MOVE)
#define MAKE_LITERAL(index, r) ((index << 16) | (r << 8) | OP_LITERAL)
#define MAKE_CALL(arg, method, result) ((arg << 24) | (method << 16) | (result << 8) | OP_CALL)
#define MAKE_RETURN(result) ((result << 24) | OP_RETURN)
#define MAKE_HACK_PRINT(r) ((r << 24) | OP_HACK_PRINT)

namespace magpie {
  typedef unsigned int bytecode;
}