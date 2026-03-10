import { createSchema, createYoga } from "graphql-yoga";
import { getAdminFirestore, verifyIdToken } from "../../lib/firebaseAdmin";

function toEpochDay(value: unknown): number | null {
  if (value == null) return null;
  if (typeof value === "number") {
    if (Number.isNaN(value)) return null;
    if (value > 1e12) return Math.floor(value / 86400_000);
    return Math.floor(value);
  }
  if (
    typeof value === "object" &&
    value !== null &&
    "toDate" in value &&
    typeof (value as { toDate: () => Date }).toDate === "function"
  ) {
    const date = (value as { toDate: () => Date }).toDate();
    return Math.floor(date.getTime() / 86400_000);
  }
  return null;
}

function epochDayToDateString(epochDay: number): string {
  return new Date(epochDay * 86400_000).toISOString().slice(0, 10);
}

const typeDefs = /* GraphQL */ `
  type Animal {
    id: ID!
    earTagNumber: String!
    rfid: String
    name: String
    sex: String!
    breed: String!
    dateOfBirth: String!
    farmId: String!
    coatColor: String
    hornStatus: String
    status: String!
    sireId: String
    damId: String
    isCastrated: Boolean!
  }

  type BreedingEvent {
    id: ID!
    animalId: String!
    sireIds: [String!]!
    eventType: String!
    serviceDate: String!
    notes: String
    pregnancyCheckDate: String
    pregnancyCheckResult: String
  }

  type Query {
    "List animals for the authenticated user. Requires Authorization: Bearer <Firebase ID token>."
    animals: [Animal!]!
    "List breeding events for the authenticated user. Requires Authorization: Bearer <Firebase ID token>."
    breedingEvents: [BreedingEvent!]!
  }
`;

async function getAnimals(uid: string) {
  const db = getAdminFirestore();
  const snap = await db.collection("users").doc(uid).collection("animals").get();
  return snap.docs.map((doc) => {
    const data = doc.data();
    const dobEpoch = toEpochDay(data.dateOfBirth);
    const dateOfBirth = dobEpoch != null ? epochDayToDateString(dobEpoch) : "";
    return {
      id: doc.id,
      earTagNumber: (data.earTagNumber as string) ?? "",
      rfid: (data.rfid as string) || null,
      name: (data.name as string) || null,
      sex: (data.sex as string) ?? "FEMALE",
      breed: (data.breed as string) ?? "",
      dateOfBirth,
      farmId: (data.farmId as string) ?? "",
      coatColor: (data.coatColor as string) || null,
      hornStatus: (data.hornStatus as string) || null,
      status: (data.status as string) ?? "ACTIVE",
      sireId: (data.sireId as string) || null,
      damId: (data.damId as string) || null,
      isCastrated: data.isCastrated === true,
    };
  });
}

async function getBreedingEvents(uid: string) {
  const db = getAdminFirestore();
  const snap = await db
    .collection("users")
    .doc(uid)
    .collection("breeding_events")
    .get();
  return snap.docs.map((doc) => {
    const data = doc.data();
    const serviceDateEpoch = toEpochDay(data.serviceDate);
    const serviceDate =
      serviceDateEpoch != null ? epochDayToDateString(serviceDateEpoch) : "";
    const pregnancyCheckDateEpoch = toEpochDay(data.pregnancyCheckDateEpochDay);
    const pregnancyCheckDate =
      pregnancyCheckDateEpoch != null
        ? epochDayToDateString(pregnancyCheckDateEpoch)
        : null;
    const sireIds = Array.isArray(data.sireIds)
      ? (data.sireIds as unknown[]).map(String).filter(Boolean)
      : [];
    return {
      id: doc.id,
      animalId: (data.animalId as string) ?? "",
      sireIds,
      eventType: (data.eventType as string) ?? "AI",
      serviceDate,
      notes: (data.notes as string) || null,
      pregnancyCheckDate,
      pregnancyCheckResult: (data.pregnancyCheckResult as string) || null,
    };
  });
}

const schema = createSchema({
  typeDefs,
  resolvers: {
    Query: {
      animals: async (_: unknown, __: unknown, context: { uid: string | null }) => {
          if (!context.uid) {
            throw new Error("Unauthorized: provide Authorization Bearer <Firebase ID token>");
          }
          try {
            return await getAnimals(context.uid);
          } catch {
            throw new Error("Service unavailable: Firebase Admin not configured or Firestore read failed");
          }
        },
        breedingEvents: async (_: unknown, __: unknown, context: { uid: string | null }) => {
          if (!context.uid) {
            throw new Error("Unauthorized: provide Authorization Bearer <Firebase ID token>");
          }
          try {
            return await getBreedingEvents(context.uid);
          } catch {
            throw new Error("Service unavailable: Firebase Admin not configured or Firestore read failed");
          }
        },
      },
    },
  });

const { handleRequest } = createYoga({
  schema,
  graphqlEndpoint: "/api/graphql",
  fetchAPI: { Response },
  context: async ({ request }): Promise<{ uid: string | null }> => {
    const authHeader = request.headers.get("Authorization");
    const decoded = await verifyIdToken(authHeader);
    return { uid: decoded?.uid ?? null };
  },
});

export const GET = (req: Request) => handleRequest(req, {});
export const POST = (req: Request) => handleRequest(req, {});
