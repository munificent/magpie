#include "Memory/Managed.h"
#include "MemoryTests.h"
#include "Memory/Memory.h"
#include "Memory/RootSource.h"

namespace magpie
{
  struct Cons : public Managed
  {
    Cons(int id) : id(id) {}

    virtual void reach()
    {
      next.reach();
    }

    int id;
    gc<Cons> next;
  };

  struct ConsRoots : public RootSource
  {
    virtual void reachRoots()
    {
      root.reach();
    }

    gc<Cons> root;
  };

  void MemoryTests::runTests()
  {
    collect();
  }

  void MemoryTests::collect()
  {
    Memory::shutDown();

    ConsRoots roots;
    Memory::initialize(&roots, sizeof(Cons) * 400);

    EXPECT_EQUAL(0, Memory::numCollections());

    gc<Cons> notRoot;

    // Make two long cons chains, only one of which is rooted.
    gc<Cons>* a = &roots.root;
    gc<Cons>* b = &notRoot;
    int id = 0;
    for (int i = 0; i <= 600; i++)
    {
      Memory::checkCollect();

      a->set(new Cons(id));
      a = &((*a)->next);

      b->set(new Cons(id));
      b = &((*b)->next);
      id++;
    }

    // Make sure the rooted ones are still valid.
    int last = -1;
    gc<Cons> c = roots.root;
    while (!c.isNull()) {
      EXPECT_EQUAL(last + 1, c->id);
      last = c->id;
      c = c->next;
    }

    // Make sure it actually did a collection.
    EXPECT(Memory::numCollections() > 0);
  }
}

